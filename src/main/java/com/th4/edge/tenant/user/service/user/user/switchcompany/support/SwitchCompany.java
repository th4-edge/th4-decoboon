package com.th4.edge.tenant.user.service.user.user.switchcompany.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("切换企业")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Setting.class, properties = {"enableStore", "gradeConfig", "brandPrimary", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel", "uscc", "qualification", "intro"})
})
@Service("user/user/switch-company")
@Scope(ScopeTypes.PROTOTYPE)
public class SwitchCompany implements com.th4.edge.tenant.user.service.user.user.switchcompany.SwitchCompany {
    @User private Employee employee;
    @AutoInject @NotNull private Long inviteId;
    @AutoInject @Length(max = 500) private String reason;
    private final TenantContext tenantContext;
    private final MembershipContext membershipContext;
    private final UserIdentityContext userContext;
    public SwitchCompany(TenantContext tenantContext, MembershipContext membershipContext,
                         UserIdentityContext userContext) {
        this.tenantContext = tenantContext; this.membershipContext = membershipContext; this.userContext = userContext;
    }
    @Override @Transactional
    public Employee call(Employee request) {
        Employee current = tenantContext.requireEmployee(employee);
        com.th4.edge.tenant.user.model.Credentials credential = userContext.requireWx(current);
        if (credential == null) throw new com.th4.edge.framework.exception.DefinedException(
                new com.th4.edge.framework.exception.DefaultErrorCode("400", "当前员工未关联微信凭证"));
        return membershipContext.accept(inviteId, credential.getIdKey(), credential.getIdSecret(),
                current.getTelNo(), current.getName(), true, reason, current.getAvatarUrl());
    }
}
