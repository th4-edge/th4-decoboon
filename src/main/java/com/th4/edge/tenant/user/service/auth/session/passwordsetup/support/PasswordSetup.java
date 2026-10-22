package com.th4.edge.tenant.user.service.auth.session.passwordsetup.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.tenant.user.context.PasswordAuthContext;
import com.th4.edge.tenant.user.context.PasswordContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.configure.EmployeeRolePolicy;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("员工首次设置密码")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"}),
        @AllowProperties(model = Credentials.class, properties = {"creId", "phone", "status", "mustChangePassword"})
})
@Service("auth/session/password-setup")
@Scope(ScopeTypes.PROTOTYPE)
public class PasswordSetup implements com.th4.edge.tenant.user.service.auth.session.passwordsetup.PasswordSetup {

    @Value("${th4.edge.framework.security.password.bootstrap-token:}")
    private String expectedBootstrapToken;

    @AutoInject
    @Length(max = 20)
    private String phone;

    @AutoInject
    @Length(max = 128)
    private String bootstrapToken;

    @AutoInject
    @Length(max = 64)
    private String password;

    @AutoInject
    @Length(max = 64)
    private String confirmPassword;

    private final PasswordAuthContext passwordAuthContext;
    private final PasswordContext passwordContext;
    private final UserIdentityContext userContext;

    public PasswordSetup(PasswordAuthContext passwordAuthContext,
                         PasswordContext passwordContext,
                         UserIdentityContext userContext) {
        this.passwordAuthContext = passwordAuthContext;
        this.passwordContext = passwordContext;
        this.userContext = userContext;
    }

    @Override
    @Transactional
    public com.th4.edge.framework.provider.security.Token call(Credentials request) {
        if (!passwordContext.bootstrapTokenMatches(expectedBootstrapToken, bootstrapToken)) {
            throw new DefinedException(new DefaultErrorCode("BOOTSTRAP_DENIED", "初始化凭证无效"));
        }
        String normalizedPhone = required(phone, "手机号");
        String initialPassword = required(password, "密码");
        if (!initialPassword.equals(confirmPassword)) {
            throw new DefinedException(new DefaultErrorCode("400", "两次输入的密码不一致"));
        }
        passwordContext.validate(initialPassword);
        Credentials credential = passwordAuthContext.requireSetupCredential(normalizedPhone);
        Employee employee = userContext.activeEmployee(credential);
        if (employee == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未加入有效企业"));
        }
        EmployeeRolePolicy.requireApplicationAccess(employee);
        passwordAuthContext.setInitialPassword(credential, initialPassword);
        return employee;
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不能为空"));
        }
        return value.trim();
    }
}
