package com.th4.edge.decoboon.customer.service.invite.accept.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.context.CustomerInviteContext;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主邀请一次性接受实现。 */
@Note("接受业主邀请")
@Properties(allow = {
        @AllowProperties(model = Credentials.class,
                properties = {"creId", "phone", "status", "lastTime", "sessionType", "customerAccesses"}),
        @AllowProperties(model = CustomerAccess.class,
                properties = {"accessId", "displayName", "status", "grantedAt", "project", "customer"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "area", "status", "progress", "property"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class,
                properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color"})
})
@Service("identity/customer-invite/accept")
@Scope(ScopeTypes.PROTOTYPE)
public class Accept implements com.th4.edge.decoboon.customer.service.invite.accept.Accept {

    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;

    @AutoInject
    private Long inviteId;

    @AutoInject
    @Length(max = 64)
    private String token;

    private final CustomerInviteContext inviteContext;
    private final CustomerAccessContext accessContext;

    public Accept(CustomerInviteContext inviteContext,
                  CustomerAccessContext accessContext) {
        this.inviteContext = inviteContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Credentials call(Credentials request) {
        Credentials credential = accessContext.requireCredential(principal);
        inviteContext.accept(inviteId, token, credential);
        accessContext.attachSession(credential);
        return credential;
    }
}
