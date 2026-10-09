package com.th4.edge.decoboon.design.service.spaces.save.support;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignFormulaRecalcContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
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

/** 方案区域保存实现。面积与周长由长宽自动计算。 */
@Note("方案区域保存")
@Properties(allow = {
        @AllowProperties(model = Space.class,
                properties = {"spaceId", "design", "type", "name", "length", "width", "height", "area",
                        "perimeter", "sort", "remark", "images", "createTime", "updateTime"}),
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
    private Integer sort;

    @AutoInject
    private String remark;

    @AutoInject
    private java.util.List<String> images;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final SpaceRepository spaceRepository;
    private final DesignRepository designRepository;
    private final DataScopeContext dataScopeContext;
    private final DesignFormulaRecalcContext formulaRecalcContext;

    public Save(DesignContext designContext, SpaceRepository spaceRepository,
                DesignRepository designRepository, DataScopeContext dataScopeContext,
                DesignFormulaRecalcContext formulaRecalcContext) {
        this.designContext = designContext;
        this.spaceRepository = spaceRepository;
        this.designRepository = designRepository;
        this.dataScopeContext = dataScopeContext;
        this.formulaRecalcContext = formulaRecalcContext;
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
        calculate(target);
        designContext.apply(target, design);
        Space saved = spaceRepository.save(target);
        updateDesignArea(design);
        designRepository.save(design);
        formulaRecalcContext.recalculate(design);
        return saved;
    }

    private Space requireSpace(Long spaceId, Employee employee) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<Space> byId = (root, query, builder) -> builder.equal(root.get("spaceId"), spaceId);
        return spaceRepository.findOne(byId.and(DataScopeSpecification.visible(Space.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "方案区域不存在或无权访问")));
    }

    private void calculate(Space space) {
        BigDecimal length = space.getLength();
        BigDecimal width = space.getWidth();
        if (length != null && width != null) {
            space.setArea(length.multiply(width).setScale(2, RoundingMode.HALF_UP));
            space.setPerimeter(length.add(width).multiply(new BigDecimal("2")).setScale(2, RoundingMode.HALF_UP));
        }
        if (space.getArea() == null) {
            space.setArea(BigDecimal.ZERO);
        }
        if (space.getPerimeter() == null) {
            space.setPerimeter(BigDecimal.ZERO);
        }
    }

    private void updateDesignArea(Design design) {
        BigDecimal total = spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId()).stream()
                .map(Space::getArea)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        design.setArea(total);
    }
}
