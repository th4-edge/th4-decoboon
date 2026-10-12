package com.th4.edge.decoboon.property.service.regions.save.support;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.context.PropertyRegionSyncContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.decoboon.property.repository.PropertyRegionRepository;
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
 * 房屋区域实测档案保存（区域实测面积/周长的唯一事实源）。
 *
 * <p>{@code DEC-064} / {@code OBJ-007}：一次录入、该房屋的多个方案复用。面积/周长可留空表示未录入
 * （不得用 0 表达未录入）；填入实测值即标 {@code AreaSource.Measured} 并记录录入人与时间。
 * 保存后触发**有界联动**：草稿方案的继承来源区域自动重算，覆盖来源与非草稿方案只计入待复核。</p>
 */
@Note("房屋区域实测档案保存")
@Description("保存房屋某区域的实测面积/周长，并联动该房屋草稿方案中继承该区域的区域")
@Properties(allow = {
        @AllowProperties(model = PropertyRegion.class,
                properties = {"regionId", "roomKey", "roomType", "name", "length", "width", "height", "area",
                        "perimeter", "areaSource", "measuredBy", "measuredAt", "sort", "remark", "createTime",
                        "updateTime", "linkedDesignCount", "pendingDesignCount"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area",
                        "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/region/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.property.service.regions.save.Save {

    @AutoInject
    @NotNull
    private Long propertyId;

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
    private final PropertyRegionRepository regionRepository;
    private final PropertyRegionSyncContext syncContext;

    public Save(PropertyContext propertyContext, PropertyRegionRepository regionRepository,
                PropertyRegionSyncContext syncContext) {
        this.propertyContext = propertyContext;
        this.regionRepository = regionRepository;
        this.syncContext = syncContext;
    }

    @Override
    @Transactional
    public PropertyRegion call(PropertyRegion request) {
        Employee current = propertyContext.requireEmployee(employee);
        Property property = propertyContext.require(propertyId, current);
        Long comId = property.getCompany().getComId();
        SpaceType type = roomType == null ? SpaceType.Bedroom : roomType;
        String normalizedName = name.trim();

        PropertyRegion target;
        if (regionId != null) {
            target = regionRepository.findByCompany_ComIdAndRegionId(comId, regionId)
                    .filter(item -> item.getProperty() != null
                            && property.getPropertyId().equals(item.getProperty().getPropertyId()))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋区域不存在或不属于该房屋")));
        } else {
            target = matchExisting(comId, property.getPropertyId(), roomKey, type, normalizedName)
                    .orElseGet(PropertyRegion::new);
        }

        // 同一房屋内区域键唯一：避免两行同键互相覆盖（公式锚点必须稳定）
        if (roomKey != null && !roomKey.isBlank()
                && !regionRepository.findByCompany_ComIdAndProperty_PropertyIdAndRoomKey(
                        comId, property.getPropertyId(), roomKey).isEmpty()
                && (target.getRegionId() == null
                        || !roomKey.equalsIgnoreCase(target.getRoomKey()))) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该房屋已存在相同区域键的区域：" + roomKey));
        }

        target.setCompany(property.getCompany());
        target.setStore(property.getStore());
        target.setTeam(property.getTeam());
        target.setOwner(property.getOwner());
        target.setProperty(property);
        target.setRoomKey(roomKey == null || roomKey.isBlank() ? null : roomKey.trim());
        target.setRoomType(type);
        target.setName(normalizedName);
        target.setLength(length);
        target.setWidth(width);
        target.setHeight(height);
        target.setSort(sort == null ? 0 : sort);
        target.setRemark(remark);
        applyMeasured(target, current);
        PropertyRegion saved = regionRepository.save(target);
        // DEC-064 第 6 条：保存后联动（草稿自动重算；覆盖与非草稿只提示）
        PropertyRegionSyncContext.Result result = syncContext.syncToDesigns(saved);
        saved.setLinkedDesignCount(result.linkedDraftDesignCount());
        saved.setPendingDesignCount(result.pendingDesignCount());
        return saved;
    }

    /**
     * 落库实测面积/周长。
     *
     * <p>{@code DEC-062} / {@code DEC-064}：以本次提交为准；两项都留空表示未录入（{@code null}），
     * 不得用 0 表达未录入；填入实测值必须 {@code > 0} 并标 {@code Measured} + 录入人/时间。</p>
     */
    private void applyMeasured(PropertyRegion region, Employee current) {
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

    /** 未显式传 regionId 时按区域键 → 「区域类型 + 标准化名称」匹配既有区域，避免重复建档。 */
    private java.util.Optional<PropertyRegion> matchExisting(Long comId, Long propertyId, String roomKey,
                                                             SpaceType type, String name) {
        java.util.List<PropertyRegion> regions = regionRepository
                .findByCompany_ComIdAndProperty_PropertyIdOrderBySortAscRegionIdAsc(comId, propertyId);
        if (roomKey != null && !roomKey.isBlank()) {
            for (PropertyRegion region : regions) {
                if (region.getRoomKey() != null && roomKey.trim().equalsIgnoreCase(region.getRoomKey())) {
                    return java.util.Optional.of(region);
                }
            }
        }
        String normalized = normalizeName(name);
        PropertyRegion matched = null;
        for (PropertyRegion region : regions) {
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
}
