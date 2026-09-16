package com.th4.edge.decoboon.property.service.properties.update.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 更新房屋基础信息。 */
@Note("房屋更新")
@Properties(allow = {
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "salesman", "designer", "latestMeasureAt", "remark", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/property/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.property.service.properties.update.Update {

    @AutoInject @NotNull private Long propertyId;
    @AutoInject private String community;
    @AutoInject private String building;
    @AutoInject private String roomNo;
    @AutoInject private String address;
    @AutoInject private BigDecimal area;
    @AutoInject private String layout;
    @AutoInject private String remark;
    @User private Employee employee;
    private final PropertyContext propertyContext;
    private final PropertyRepository propertyRepository;

    public Update(PropertyContext propertyContext, PropertyRepository propertyRepository) {
        this.propertyContext = propertyContext;
        this.propertyRepository = propertyRepository;
    }

    @Override
    @Transactional
    public Property call(Property request) {
        Property property = propertyContext.require(propertyId, employee);
        if (community != null) property.setCommunity(community);
        if (building != null) property.setBuilding(building);
        if (roomNo != null) property.setRoomNo(roomNo);
        if (address != null && !address.isBlank()) property.setAddress(address.trim());
        if (area != null) property.setArea(area);
        if (layout != null) property.setLayout(layout);
        if (remark != null) property.setRemark(remark);
        return propertyRepository.save(property);
    }
}
