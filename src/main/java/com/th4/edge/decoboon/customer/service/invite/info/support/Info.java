package com.th4.edge.decoboon.customer.service.invite.info.support;

import com.th4.edge.decoboon.customer.context.CustomerInviteContext;
import com.th4.edge.decoboon.customer.model.Customer;
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
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 通过 secret token 或“邀请 ID + 手机号”查询邀请，不回传完整手机号。 */
@Note("业主邀请查询")
@Properties(allow = {
        @AllowProperties(model = CustomerInvite.class,
                properties = {"inviteId", "company", "project", "customer", "inviter", "customerName", "maskedTelNo",
                        "status", "expireTime", "acceptedAt", "cancelledAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name", "property", "customer"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("identity/customer-invite/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.invite.info.Info {

    @AutoInject
    private Long inviteId;

    @AutoInject
    @Length(max = 64)
    private String token;

    @AutoInject
    @Length(max = 20)
    private String phone;

    private final CustomerInviteContext inviteContext;

    public Info(CustomerInviteContext inviteContext) {
        this.inviteContext = inviteContext;
    }

    @Override
    public CustomerInvite call(CustomerInvite request) {
        return inviteContext.requireInfo(inviteId, token, phone);
    }
}
