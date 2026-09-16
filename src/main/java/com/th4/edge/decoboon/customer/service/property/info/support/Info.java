package com.th4.edge.decoboon.customer.service.property.info.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 业主房屋详情实现。 */
@Note("业主房屋详情")
@Properties(allow = @AllowProperties(model = Property.class,
        properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area",
                "layout", "status", "latestMeasureAt"}))
@Service("customer/property/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.property.info.Info {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    private final CustomerAccessContext accessContext;

    public Info(CustomerAccessContext accessContext) {
        this.accessContext = accessContext;
    }

    @Override
    public Property call(Property request) {
        Credentials credential = accessContext.requireCredential(principal);
        return accessContext.requireProject(credential, projectId).getProperty();
    }
}
