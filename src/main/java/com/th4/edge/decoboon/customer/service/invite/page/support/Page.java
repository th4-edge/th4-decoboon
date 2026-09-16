package com.th4.edge.decoboon.customer.service.invite.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerInvite;
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
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** 项目内业主邀请分页实现。 */
@Note("业主邀请分页")
@Properties(allow = {
        @AllowProperties(model = CustomerInvite.class,
                properties = {"inviteId", "project", "customer", "inviter", "customerName", "telNo", "token",
                        "status", "expireTime", "acceptedAt", "cancelledAt", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "property"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("identity/customer-invite/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.invite.page.Page {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CustomerInviteRepository inviteRepository;
    private final ProjectAccessContext accessContext;

    public Page(CustomerInviteRepository inviteRepository,
                ProjectAccessContext accessContext) {
        this.inviteRepository = inviteRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<CustomerInvite> call(CustomerInvite request) {
        accessContext.requireProject(projectId, employee);
        var invites = inviteRepository.findByProject_ProjectIdOrderByInviteIdDesc(projectId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, invites.size());
        int to = Math.min(from + pageSize, invites.size());
        return new PageImpl<>(invites.subList(from, to), PageRequest.of(page, pageSize), invites.size());
    }
}
