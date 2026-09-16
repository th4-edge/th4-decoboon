package com.th4.edge.tenant.user.service.company.member.info.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.context.MembershipContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
@Note("成员详情")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "resignedAt", "resignReason", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("company/member/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.tenant.user.service.company.member.info.Info {
    @User private Employee employee;
    @AutoInject @NotNull private Long empId;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    public Info(TenantContext tenantContext, MembershipContext membershipContext) { this.tenantContext = tenantContext; this.membershipContext = membershipContext; }
    @Override
    public Employee call(Employee request) {
        Employee current = tenantContext.requireEmployee(employee);
        return membershipContext.memberInfo(current, empId);
    }
}
