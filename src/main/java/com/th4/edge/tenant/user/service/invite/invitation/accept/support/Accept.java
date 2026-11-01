package com.th4.edge.tenant.user.service.invite.invitation.accept.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("接受邀请")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Credentials.class, properties = {"creId", "openId", "unionId", "phone", "status", "employee"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Setting.class, properties = {"enableStore", "gradeConfig", "brandPrimary", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel", "uscc", "qualification", "intro"})
})
@Service("invite/invitation/accept")
@Scope(ScopeTypes.PROTOTYPE)
public class Accept implements com.th4.edge.tenant.user.service.invite.invitation.accept.Accept {
    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;
    @AutoInject @NotNull private Long inviteId;
    @AutoInject @Length(max = 15) private String name;
    @AutoInject @Length(max = 512) private String avatarUrl;
    @AutoInject private Boolean confirmSwitch;
    @AutoInject @Length(max = 500) private String reason;
    private final MembershipContext membershipContext;
    private final EmployeeRepository employeeRepository;
    private final UserIdentityContext userContext;
    public Accept(MembershipContext membershipContext, EmployeeRepository employeeRepository,
                  UserIdentityContext userContext) {
        this.membershipContext = membershipContext;
        this.employeeRepository = employeeRepository;
        this.userContext = userContext;
    }
    @Override @Transactional
    public Employee call(Employee request) {
        Credentials credential;
        if (principal instanceof Employee tokenEmployee) {
            Employee employee = employeeRepository.findByEmpId(tokenEmployee.getEmpId());
            credential = userContext.requireWx(employee);
        } else {
            credential = userContext.requireUser(Long.valueOf(principal.getUserId().toString()));
        }
        if (credential == null) throw new com.th4.edge.framework.exception.DefinedException(
                new com.th4.edge.framework.exception.DefaultErrorCode("401", "当前微信凭证不存在"));
        return membershipContext.accept(inviteId, credential.getIdKey(), credential.getIdSecret(), credential.getPhone(),
                name, Boolean.TRUE.equals(confirmSwitch), reason, avatarUrl);
    }
}
