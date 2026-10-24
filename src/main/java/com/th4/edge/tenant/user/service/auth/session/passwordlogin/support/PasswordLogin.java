package com.th4.edge.tenant.user.service.auth.session.passwordlogin.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.tenant.user.context.PasswordAuthContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.configure.EmployeeRolePolicy;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("员工密码登录")
@Properties(allow = {
        @AllowProperties(model = Credentials.class, properties = {"creId", "phone", "status", "lastTime", "mustChangePassword"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"})
})
@Service("auth/session/password-login")
@Scope(ScopeTypes.PROTOTYPE)
public class PasswordLogin implements com.th4.edge.tenant.user.service.auth.session.passwordlogin.PasswordLogin {

    @AutoInject
    @Length(max = 20)
    private String phone;

    @AutoInject
    @Length(max = 64)
    private String password;

    private final PasswordAuthContext passwordAuthContext;
    private final UserIdentityContext userContext;

    public PasswordLogin(PasswordAuthContext passwordAuthContext, UserIdentityContext userContext) {
        this.passwordAuthContext = passwordAuthContext;
        this.userContext = userContext;
    }

    @Override
    @Transactional
    public com.th4.edge.framework.provider.security.Token call(Credentials request) {
        String normalizedPhone = required(phone, "手机号");
        Credentials credential = passwordAuthContext.lockLoginCredential(normalizedPhone);
        if (credential == null) {
            userContext.logUnknown(normalizedPhone, com.th4.edge.tenant.user.configure.SecurityAction.Login,
                    com.th4.edge.tenant.user.configure.SecurityResult.Failure, "账号不存在或未设置密码");
            throw new DefinedException(new DefaultErrorCode("AUTH_FAILED", "手机号或密码错误"));
        }
        Employee employee = userContext.activeEmployee(credential);
        if (employee == null) {
            passwordAuthContext.loginFailed(credential, "员工不在职");
            throw new DefinedException(new DefaultErrorCode("403", "当前账号已停用或未加入企业"));
        }
        if (!passwordAuthContext.matches(required(password, "密码"), credential)) {
            passwordAuthContext.loginFailed(credential, "密码错误");
            throw new DefinedException(new DefaultErrorCode("AUTH_FAILED", "手机号或密码错误"));
        }
        EmployeeRolePolicy.requireApplicationAccess(employee);
        passwordAuthContext.loginSucceeded(credential);
        return employee;
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不能为空"));
        }
        return value.trim();
    }
}
