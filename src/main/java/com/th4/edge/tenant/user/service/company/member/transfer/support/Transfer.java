package com.th4.edge.tenant.user.service.company.member.transfer.support;
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
@Note("成员任务交接")
@Properties(allow = {
        @AllowProperties(model = MemberHandover.class, properties = {"employee", "customerCount", "propertyCount", "quoteCount", "projectCount", "taskCount", "receivableCount"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role", "status", "company", "store", "team"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Store.class, properties = {"storeId", "name", "status", "defaultStore", "sort", "color", "addr"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Team.class, properties = {"teamId", "name", "color"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Setting.class, properties = {"enableStore", "gradeConfig", "brandPrimary", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel", "uscc", "qualification", "intro"})
})
@Service("company/member/transfer")
@Scope(ScopeTypes.PROTOTYPE)
public class Transfer implements com.th4.edge.tenant.user.service.company.member.transfer.Transfer {
    @User private Employee employee;
    @AutoInject @NotNull private Long fromEmpId;
    @AutoInject @NotNull private Long toEmpId;
    @AutoInject private Boolean transferProjects;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    public Transfer(TenantContext tenantContext, MembershipContext membershipContext) { this.tenantContext = tenantContext; this.membershipContext = membershipContext; }
    @Override @Transactional
    public MemberHandover call(Employee request) {
        return membershipContext.transfer(tenantContext.requireEmployee(employee), fromEmpId, toEmpId, Boolean.TRUE.equals(transferProjects));
    }
}
