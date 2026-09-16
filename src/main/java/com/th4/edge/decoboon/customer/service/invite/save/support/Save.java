package com.th4.edge.decoboon.customer.service.invite.save.support;

import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.customer.context.CustomerInviteContext;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/** 业主同频邀请创建实现。 */
@Note("业主邀请创建")
@Properties(allow = {
        @AllowProperties(model = CustomerInvite.class,
                properties = {"inviteId", "project", "customer", "inviter", "customerName", "telNo", "token",
                        "status", "expireTime", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name", "property", "customer" }),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr"})
})
@Service("identity/customer-invite/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.invite.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    @NotEmpty
    @Length(max = 32)
    private String customerName;

    @AutoInject
    @NotEmpty
    @Length(max = 20)
    private String telNo;

    private final CustomerInviteRepository inviteRepository;
    private final ProjectAccessContext accessContext;

    public Save(CustomerInviteRepository inviteRepository,
                ProjectAccessContext accessContext) {
        this.inviteRepository = inviteRepository;
        this.accessContext = accessContext;
    }

    @Override
    public CustomerInvite call(CustomerInvite request) {
        Employee current = accessContext.requireEmployee(employee);
        Project project = accessContext.requireProject(projectId, current);
        CustomerInvite invite = new CustomerInvite();
        invite.setCompany(project.getCompany());
        invite.setProject(project);
        invite.setCustomer(project.getCustomer());
        invite.setInviter(current);
        invite.setCustomerName(customerName);
        invite.setTelNo(CustomerInviteContext.normalizePhone(telNo));
        invite.setToken(UUID.randomUUID().toString().replace("-", ""));
        invite.setStatus(InviteStatus.Pending);
        invite.setExpireTime(LocalDateTime.now().plusDays(30));
        return inviteRepository.save(invite);
    }
}
