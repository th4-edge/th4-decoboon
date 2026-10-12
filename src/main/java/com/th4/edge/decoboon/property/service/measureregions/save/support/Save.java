package com.th4.edge.decoboon.property.service.measureregions.save.support;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 量房原始区域保存。
 *
 * <p>{@code DEC-066}：原始区域是**量房期的原始结构事实**，量房单完成后**冻结**——
 * 已完成的量房单只读，需要修正必须重新量房（复尺）。面积/周长可留空表示未录入
 * （不得用 0 表达）；填入实测值即标 {@code Measured} 并记录录入人与时间（{@code DEC-062}）。</p>
 */
@Note("量房原始区域保存")
@Description("在量房单上维护房屋原始结构的区域实测面积/周长；量房完成后冻结")
@Properties(allow = {
        @AllowProperties(model = MeasureRegion.class,
                properties = {"regionId", "roomKey", "roomType", "name", "length", "width", "height", "area",
                        "perimeter", "areaSource", "measuredBy", "measuredAt", "sort", "remark", "createTime",
                        "updateTime"}),
        @AllowProperties(model = Measure.class,
                properties = {"measureId", "status", "measureTime", "completedAt", "scheduledAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/measure/region/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.property.service.measureregions.save.Save {

    @AutoInject
    @NotNull
    private Long measureId;

    @AutoInject
    private Long regionId;

    @AutoInject
    private String roomKey;

    @AutoInject
    private SpaceType roomType;

    @AutoInject
    @NotEmpty
    private String name;

    @AutoInject
    private BigDecimal length;

    @AutoInject
    private BigDecimal width;

    @AutoInject
    private BigDecimal height;

    @AutoInject
    private BigDecimal area;

    @AutoInject
    private BigDecimal perimeter;

    @AutoInject
    private Integer sort;

    @AutoInject
    private String remark;

    @User
    private Employee employee;

    private final PropertyContext propertyContext;
    private final MeasureRepository measureRepository;
    private final MeasureRegionRepository regionRepository;

    public Save(PropertyContext propertyContext, MeasureRepository measureRepository,
                MeasureRegionRepository regionRepository) {
        this.propertyContext = propertyContext;
        this.measureRepository = measureRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional
    public MeasureRegion call(MeasureRegion request) {
        Employee current = propertyContext.requireEmployee(employee);
        Measure measure = requireMeasure(current);
        requireNotFrozen(measure);

        Long comId = measure.getCompany().getComId();
        MeasureRegion target;
        if (regionId != null) {
            target = regionRepository.findByCompany_ComIdAndRegionId(comId, regionId)
                    .filter(item -> item.getMeasure() != null
                            && measure.getMeasureId().equals(item.getMeasure().getMeasureId()))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "量房原始区域不存在或不属于该量房单")));
        } else {
            target = matchExisting(comId, measure.getMeasureId(), roomKey, roomType, name.trim())
                    .orElseGet(MeasureRegion::new);
        }

        target.setCompany(measure.getCompany());
        target.setStore(measure.getStore());
        target.setTeam(measure.getTeam());
        target.setOwner(measure.getOwner());
        target.setMeasure(measure);
        target.setProperty(measure.getProperty());
        target.setRoomKey(roomKey == null || roomKey.isBlank() ? null : roomKey.trim());
        target.setRoomType(roomType == null ? SpaceType.Bedroom : roomType);
        target.setName(name.trim());
        target.setLength(length);
        target.setWidth(width);
        target.setHeight(height);
        target.setSort(sort == null ? 0 : sort);
        target.setRemark(remark);
        applyMeasured(target, current);
        return regionRepository.save(target);
    }

    /**
     * 量房完成后冻结（{@code DEC-066}）。
     *
     * <p>原始结构一旦量完就是事实；要修正必须重新量房（复尺），不得在已完成的量房单上改写，
     * 否则历史方案的起点会与当时的量房事实不一致。</p>
     */
    private void requireNotFrozen(Measure measure) {
        if (measure.getStatus() == MeasureStatus.Completed) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该量房已完成，原始结构不可再修改；如需修正请重新量房（复尺）"));
        }
    }

    private void applyMeasured(MeasureRegion region, Employee current) {
        if (area == null && perimeter == null) {
            region.setArea(null);
            region.setPerimeter(null);
            region.setAreaSource(null);
            region.setMeasuredBy(null);
            region.setMeasuredAt(null);
            return;
        }
        if (area != null) {
            requirePositive(area, "实测面积");
            region.setArea(area.setScale(2, RoundingMode.HALF_UP));
        } else {
            region.setArea(null);
        }
        if (perimeter != null) {
            requirePositive(perimeter, "实测周长");
            region.setPerimeter(perimeter.setScale(2, RoundingMode.HALF_UP));
        } else {
            region.setPerimeter(null);
        }
        region.setAreaSource(AreaSource.Measured);
        region.setMeasuredBy(current);
        region.setMeasuredAt(LocalDateTime.now());
    }

    private void requirePositive(BigDecimal value, String label) {
        if (value.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", label + "必须大于 0（留空表示未录入）"));
        }
    }

    private java.util.Optional<MeasureRegion> matchExisting(Long comId, Long measureId, String roomKey,
                                                            SpaceType type, String name) {
        java.util.List<MeasureRegion> regions = regionRepository
                .findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(comId, measureId);
        if (roomKey != null && !roomKey.isBlank()) {
            for (MeasureRegion region : regions) {
                if (region.getRoomKey() != null && roomKey.trim().equalsIgnoreCase(region.getRoomKey())) {
                    return java.util.Optional.of(region);
                }
            }
        }
        String normalized = normalizeName(name);
        MeasureRegion matched = null;
        for (MeasureRegion region : regions) {
            if (region.getRoomType() == type && normalizeName(region.getName()).equals(normalized)) {
                if (matched != null) {
                    return java.util.Optional.empty();
                }
                matched = region;
            }
        }
        return java.util.Optional.ofNullable(matched);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private Measure requireMeasure(Employee current) {
        Measure measure = measureRepository.findById(measureId)
                .filter(item -> item.getCompany() != null && current.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "量房记录不存在或无权访问")));
        if (measure.getProperty() != null && !propertyContext.canView(measure.getProperty(), current)) {
            throw new DefinedException(new DefaultErrorCode("404", "量房记录不存在或无权访问"));
        }
        return measure;
    }
}
