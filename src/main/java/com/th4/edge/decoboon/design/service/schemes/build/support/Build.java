package com.th4.edge.decoboon.design.service.schemes.build.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
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
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** 设计方案状态构建实现。 */
@Note("设计方案构建")
@Properties(allow = {
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "style", "status", "area", "layout",
                        "coverUrl", "cadUrl", "owner", "customer", "updateTime", "property"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role"})
})
@Service("design/scheme/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.design.service.schemes.build.Build {

    @AutoInject
    @NotNull
    private Long designId;

    @AutoInject
    @NotNull
    private DesignStatus status;

    @User
    private Employee employee;

    private final DesignContext designContext;
    private final DesignRepository designRepository;
    private final SpaceRepository spaceRepository;
    private final DesignItemRepository itemRepository;

    public Build(DesignContext designContext,
                 DesignRepository designRepository,
                 SpaceRepository spaceRepository,
                 DesignItemRepository itemRepository) {
        this.designContext = designContext;
        this.designRepository = designRepository;
        this.spaceRepository = spaceRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public Design call(Design request) {
        Design design = designContext.requireDesign(designId, employee);
        designContext.requireEditable(design);
        if (status == DesignStatus.Ready || status == DesignStatus.Presented || status == DesignStatus.Selected) {
            List<Space> spaces = spaceRepository.findByDesign_DesignIdOrderBySortAsc(designId);
            if (itemRepository.findByDesign_DesignIdOrderBySortAsc(designId).isEmpty()) {
                throw new DefinedException(new DefaultErrorCode("400", "请先配置施工或材料分项"));
            }
            // 区域是可选的设计组织维度：允许无区域但有综合项目的方案完成；
            // 有区域时按区域**实测面积**汇总（DEC-062：估算值不参与），无区域时保留方案已有面积。
            if (!spaces.isEmpty()) {
                design.setArea(spaces.stream()
                        .filter(space -> space.getAreaSource() == com.th4.edge.decoboon.design.configure.AreaSource.Measured)
                        .map(Space::getArea)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        }
        design.setStatus(status);
        return designRepository.save(design);
    }
}
