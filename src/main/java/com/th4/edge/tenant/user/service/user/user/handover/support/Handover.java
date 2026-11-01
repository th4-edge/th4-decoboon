package com.th4.edge.tenant.user.service.user.user.handover.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.MemberHandover;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("交接清单")
@Properties(allow = {
        @AllowProperties(model = MemberHandover.class, properties = {"employee", "customerCount", "propertyCount", "quoteCount", "projectCount", "taskCount", "receivableCount"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role", "status", "company", "store", "team"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Store.class, properties = {"storeId", "name", "status", "defaultStore", "sort", "color", "addr"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Team.class, properties = {"teamId", "name", "color"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Setting.class, properties = {"enableStore", "gradeConfig", "brandPrimary", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel", "uscc", "qualification", "intro"})
})
@Service("user/user/handover")
@Scope(ScopeTypes.PROTOTYPE)
public class Handover implements com.th4.edge.tenant.user.service.user.user.handover.Handover {
    @User private Employee employee;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    public Handover(TenantContext tenantContext, MembershipContext membershipContext) {
        this.tenantContext = tenantContext; this.membershipContext = membershipContext;
    }
    @Override @Transactional
    public MemberHandover call(Employee request) { return membershipContext.handover(tenantContext.requireEmployee(employee)); }
}
