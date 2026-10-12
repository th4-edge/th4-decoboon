package com.th4.edge.decoboon.design.service.spaces.save.support;

import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.configure.SpaceValueOrigin;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignFormulaRecalcContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.property.context.PropertyRegionSyncContext;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.decoboon.property.repository.PropertyRegionRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** 方案区域保存实现。面积与周长是设计师实测录入值，服务端不推算（BR-040 §4 / DEC-062）。 */
@Note("方案区域保存")
@Properties(allow = {
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "design", "type", "name", "length", "width", "height", "area",
                        "perimeter", "areaSource", "valueOrigin", "houseRegionId", "inheritedArea", "inheritedPerimeter", "houseValueStale", "sort", "remark", "images", "createTime", "updateTime"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "status", "area", "layout"}),
})
@Service("design/space/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.design.service.spaces.save.Save {

    @AutoInject
    @NotNull
    private Long designId;

    @AutoInject
    private Long spaceId;

    @AutoInject
    private com.th4.edge.decoboon.bim.configure.SpaceType type;

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

    @AutoInject
    private java.util.List<String> images;

    @AutoInject
    private Long houseRegionId;

    /** 显式同步：从关联的房屋区域实测档案取当前实测值并覆盖本区域（DEC-064）。 */
    @AutoInject
    private Boolean syncFromHouseRegion;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final SpaceRepository spaceRepository;
    private final DesignRepository designRepository;
    private final DataScopeContext dataScopeContext;
    private final DesignFormulaRecalcContext formulaRecalcContext;
    private final PropertyRegionRepository regionRepository;
    private final PropertyRegionSyncContext regionSyncContext;

    public Save(DesignContext designContext, SpaceRepository spaceRepository,
                DesignRepository designRepository, DataScopeContext dataScopeContext,
                DesignFormulaRecalcContext formulaRecalcContext,
                PropertyRegionRepository regionRepository,
                PropertyRegionSyncContext regionSyncContext) {
        this.designContext = designContext;
        this.spaceRepository = spaceRepository;
        this.designRepository = designRepository;
        this.dataScopeContext = dataScopeContext;
        this.formulaRecalcContext = formulaRecalcContext;
        this.regionRepository = regionRepository;
        this.regionSyncContext = regionSyncContext;
    }

    @Override
    @Transactional
    public Space call(Space request) {
        Design design = designContext.requireDesign(designId, employee);
        designContext.requireEditable(design);
        Space target = spaceId == null ? new Space() : requireSpace(spaceId, employee);
        target.setType(type == null ? com.th4.edge.decoboon.bim.configure.SpaceType.LivingRoom : type);
        target.setName(name);
        target.setLength(length);
        target.setWidth(width);
        target.setHeight(height);
        target.setSort(sort == null ? 0 : sort);
        target.setRemark(remark);
        target.setImages(images);
        if (houseRegionId != null) {
            target.setHouseRegionId(houseRegionId);
        }
        Employee current = dataScopeContext.requireEmployee(employee);
        if (Boolean.TRUE.equals(syncFromHouseRegion)) {
            syncFromHouse(design, target);
        } else {
            applyMeasured(target);
            applyValueOrigin(target, current, design);
        }
        designContext.apply(target, design);
        Space saved = spaceRepository.save(target);
        // 方案面积与公式分项统一由重算上下文处理（DEC-062），避免两处口径
        formulaRecalcContext.recalculate(design);
        designRepository.save(design);
        return saved;
    }

    private Space requireSpace(Long spaceId, Employee employee) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<Space> byId = (root, query, builder) -> builder.equal(root.get("spaceId"), spaceId);
        return spaceRepository.findOne(byId.and(DataScopeSpecification.visible(Space.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "方案区域不存在或无权访问")));
    }

    /**
     * 落库区域实测面积/周长。
     *
     * <p>BR-040 §4 / {@code DEC-062}（{@code EV-213}）：区域面积与周长只能由设计师实测量房后经 CAD
     * 计算录入，服务端**一律不推算**；长宽只作为量房快照保存，变化不触发面积/周长重算。</p>
     *
     * <p>未提供的值表示「未录入」而不是 0：新建区域保持为空，公式引用 {@code A/P} 的分项按「待补尺寸」
     * 处理。历史按长宽推算的估算值（{@code Estimated}）不允许继续充当工程量基数，因此首次录入实测值时
     * 以本次提交为准，未同步提交的那一项按未录入处理。</p>
     */
    private void applyMeasured(Space space) {
        boolean firstMeasured = space.getAreaSource() != AreaSource.Measured;
        if (firstMeasured) {
            space.setArea(null);
            space.setPerimeter(null);
        }
        if (area != null) {
            requirePositive(area, "实测面积");
            space.setArea(area.setScale(2, RoundingMode.HALF_UP));
        }
        if (perimeter != null) {
            requirePositive(perimeter, "实测周长");
            space.setPerimeter(perimeter.setScale(2, RoundingMode.HALF_UP));
        }
        if (space.getArea() != null || space.getPerimeter() != null) {
            space.setAreaSource(AreaSource.Measured);
        }
    }

    /**
     * 显式同步房屋实测值（{@code DEC-064}）：把关联房屋区域档案的当前实测值写回本区域，
     * 来源标记为「房屋档案继承」，并刷新房屋值快照用于后续变更提示。
     */
    private void syncFromHouse(Design design, Space space) {
        Long regionId = space.getHouseRegionId();
        if (regionId == null) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该区域未关联房屋区域实测档案，无法同步房屋实测值"));
        }
        Long propertyId = design.getProperty() == null ? null : design.getProperty().getPropertyId();
        PropertyRegion region = regionRepository
                .findByCompany_ComIdAndRegionId(design.getCompany().getComId(), regionId)
                .filter(item -> item.getProperty() != null && propertyId != null
                        && propertyId.equals(item.getProperty().getPropertyId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404",
                        "房屋区域实测档案不存在或不属于本方案的房屋")));
        regionSyncContext.applyFromHouse(space, region);
    }

    /**
     * 记录方案区域取值的来源与留痕（{@code DEC-064}）。
     *
     * <p>设计师在方案内提交实测值即视为「方案覆盖」（可能是现场修正，也可能是该区域未关联房屋档案）；
     * 已关联房屋区域时同时刷新房屋值快照，用于「房屋实测值已变更」提示。未录入则清空来源与留痕。</p>
     */
    private void applyValueOrigin(Space space, Employee current, Design design) {
        if (space.getAreaSource() != AreaSource.Measured) {
            space.setValueOrigin(null);
            space.setOverrideBy(null);
            space.setOverrideAt(null);
            return;
        }
        space.setValueOrigin(SpaceValueOrigin.Override);
        space.setOverrideBy(current);
        space.setOverrideAt(LocalDateTime.now());
        if (space.getHouseRegionId() != null && design.getCompany() != null) {
            regionRepository.findByCompany_ComIdAndRegionId(design.getCompany().getComId(), space.getHouseRegionId())
                    .ifPresent(region -> {
                        space.setInheritedArea(region.getArea());
                        space.setInheritedPerimeter(region.getPerimeter());
                    });
        }
    }

    private void requirePositive(BigDecimal value, String label) {
        if (value.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", label + "必须大于 0（实测值由设计师按 CAD 录入）"));
        }
    }

}
