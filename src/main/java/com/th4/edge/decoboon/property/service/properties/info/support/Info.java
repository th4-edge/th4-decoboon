package com.th4.edge.decoboon.property.service.properties.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
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

/** 房屋详情。 */
@Note("房屋详情")
@Properties(allow = {
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer", "latestMeasureAt", "remark", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/property/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.property.service.properties.info.Info {

    @AutoInject
    @NotNull
    private Long propertyId;

    @User
    private Employee employee;

    private final PropertyContext propertyContext;

    public Info(PropertyContext propertyContext) {
        this.propertyContext = propertyContext;
    }

    @Override
    public Property call(Property request) {
        return propertyContext.require(propertyId, employee);
    }
}
