package com.th4.edge.tenant.user.service.company.member.handover.support;
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
import com.th4.edge.tenant.user.model.MemberHandover;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("成员交接清单")
@Properties(allow = {
        @AllowProperties(model = MemberHandover.class, properties = {"employee", "customerCount", "propertyCount", "quoteCount", "projectCount", "taskCount", "receivableCount"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role", "status", "company", "store", "team"})
})
@Service("company/member/handover")
@Scope(ScopeTypes.PROTOTYPE)
public class Handover implements com.th4.edge.tenant.user.service.company.member.handover.Handover {
    @User private Employee employee;
    @AutoInject @NotNull private Long empId;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    public Handover(TenantContext tenantContext, MembershipContext membershipContext) { this.tenantContext = tenantContext; this.membershipContext = membershipContext; }
    @Override @Transactional
    public MemberHandover call(Employee request) { return membershipContext.handoverFor(tenantContext.requireEmployee(employee), empId); }
}
