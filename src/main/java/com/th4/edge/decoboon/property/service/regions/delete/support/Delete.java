package com.th4.edge.decoboon.property.service.regions.delete.support;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
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
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 房屋区域实测档案删除。
 *
 * <p>{@code DEC-064}：被**草稿方案**引用时阻断并提示先解除关联或改为方案内录入，
 * 避免草稿方案的实测值来源凭空消失。</p>
 */
@Note("房屋区域实测档案删除")
@Properties(allow = {
        @AllowProperties(model = PropertyRegion.class,
                properties = {"regionId", "roomKey", "roomType", "name", "area", "perimeter", "areaSource", "sort"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "address"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/region/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.property.service.regions.delete.Delete {

    @AutoInject
    @NotNull
    private Long regionId;

    @User
    private Employee employee;

    private final PropertyContext propertyContext;
    private final PropertyRegionRepository regionRepository;
    private final SpaceRepository spaceRepository;

    public Delete(PropertyContext propertyContext, PropertyRegionRepository regionRepository,
                  SpaceRepository spaceRepository) {
        this.propertyContext = propertyContext;
        this.regionRepository = regionRepository;
        this.spaceRepository = spaceRepository;
    }

    @Override
    @Transactional
    public PropertyRegion call(PropertyRegion request) {
        Employee current = propertyContext.requireEmployee(employee);
        PropertyRegion region = regionRepository.findById(regionId)
                .filter(item -> item.getCompany() != null && current.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .filter(item -> item.getProperty() != null && propertyContext.canView(item.getProperty(), current))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋区域不存在或无权访问")));

        List<Space> referenced = spaceRepository.findByHouseRegionId(region.getRegionId());
        long draftReferenced = referenced.stream()
                .filter(space -> space.getDesign() != null
                        && (space.getDesign().getStatus() == null
                        || space.getDesign().getStatus() == DesignStatus.Draft))
                .count();
        if (draftReferenced > 0) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该房屋区域已被 " + draftReferenced + " 个草稿方案引用，请先在方案中解除关联或改为方案内录入实测值"));
        }
        regionRepository.delete(region);
        return region;
    }
}
