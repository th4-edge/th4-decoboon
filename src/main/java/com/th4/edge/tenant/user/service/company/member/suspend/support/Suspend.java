package com.th4.edge.tenant.user.service.company.member.suspend.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("suspend 员工")
@Properties(allow = @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "resignedAt", "resignReason"}))
@Service("company/member/suspend")
@Scope(ScopeTypes.PROTOTYPE)
public class Suspend implements com.th4.edge.tenant.user.service.company.member.suspend.Suspend {
    @User private Employee employee;
    @AutoInject @NotNull private Long empId;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    public Suspend(TenantContext tenantContext, MembershipContext membershipContext) { this.tenantContext = tenantContext; this.membershipContext = membershipContext; }
    @Override @Transactional
    public Employee call(Employee request) { return membershipContext.suspend(tenantContext.requireEmployee(employee), empId); }
}
