package com.th4.edge.tenant.user.service.company.member.resign.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.MemberHandover;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("注销员工")
@Properties(allow = {
        @AllowProperties(model = MemberHandover.class, properties = {"employee", "customerCount", "propertyCount", "quoteCount", "projectCount", "taskCount", "receivableCount"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role", "status", "company", "store", "team"})
})
@Service("company/member/resign")
@Scope(ScopeTypes.PROTOTYPE)
public class Resign implements com.th4.edge.tenant.user.service.company.member.resign.Resign {
    @User private Employee employee;
    @AutoInject @NotNull private Long empId;
    @AutoInject @Length(max = 500) private String reason;
    @AutoInject private Boolean forceHandover;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    public Resign(TenantContext tenantContext, MembershipContext membershipContext) { this.tenantContext = tenantContext; this.membershipContext = membershipContext; }
    @Override @Transactional
    public MemberHandover call(Employee request) { return membershipContext.adminResign(tenantContext.requireEmployee(employee), empId, reason, Boolean.TRUE.equals(forceHandover)); }
}
