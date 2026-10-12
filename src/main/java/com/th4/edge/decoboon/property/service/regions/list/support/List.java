package com.th4.edge.decoboon.property.service.regions.list.support;

import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.decoboon.property.repository.PropertyRegionRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 房屋区域实测档案列表实现。 */
@Note("房屋区域实测档案列表")
@Properties(allow = {
        @AllowProperties(model = PropertyRegion.class,
                properties = {"regionId", "roomKey", "roomType", "name", "length", "width", "height", "area",
                        "perimeter", "areaSource", "measuredBy", "measuredAt", "sort", "remark", "createTime",
                        "updateTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area",
                        "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/region/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.property.service.regions.list.List {

    @AutoInject
    @NotNull
    private Long propertyId;

    @User
    private Employee employee;

    private final PropertyContext propertyContext;
    private final PropertyRegionRepository regionRepository;

    public List(PropertyContext propertyContext, PropertyRegionRepository regionRepository) {
        this.propertyContext = propertyContext;
        this.regionRepository = regionRepository;
    }

    @Override
    public java.util.List<PropertyRegion> call(PropertyRegion request) {
        Employee current = propertyContext.requireEmployee(employee);
        Property property = propertyContext.require(propertyId, current);
        return regionRepository.findByCompany_ComIdAndProperty_PropertyIdOrderBySortAscRegionIdAsc(
                property.getCompany().getComId(), property.getPropertyId());
    }
}
