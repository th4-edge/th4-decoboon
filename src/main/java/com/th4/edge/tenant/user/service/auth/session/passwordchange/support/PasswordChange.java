package com.th4.edge.tenant.user.service.auth.session.passwordchange.support;

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
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("设置或修改密码")
@Properties(allow = {
        @AllowProperties(model = Credentials.class, properties = {"creId", "phone", "status", "lastTime", "passwordSet", "mustChangePassword"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"})
})
@Service("auth/session/password-change")
@Scope(ScopeTypes.PROTOTYPE)
public class PasswordChange implements com.th4.edge.tenant.user.service.auth.session.passwordchange.PasswordChange {

    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;

    @AutoInject
    @Length(max = 64)
    private String oldPassword;

    @AutoInject
    @Length(max = 64)
    private String newPassword;

    @AutoInject
    @Length(max = 64)
    private String confirmPassword;

    private final PasswordAuthContext passwordAuthContext;
    private final PasswordContext passwordContext;
    private final UserIdentityContext userContext;

    public PasswordChange(PasswordAuthContext passwordAuthContext,
                          PasswordContext passwordContext,
                          UserIdentityContext userContext) {
        this.passwordAuthContext = passwordAuthContext;
        this.passwordContext = passwordContext;
        this.userContext = userContext;
    }

    @Override
    @Transactional
    public Credentials call(Employee request) {
        Credentials credential;
        if (principal instanceof Employee employee) {
            credential = userContext.requireWx(employee);
        } else if (principal instanceof Credentials credentials) {
            credential = userContext.requireUser(credentials.getCreId());
        } else {
            credential = null;
        }
        if (credential == null) {
            throw new DefinedException(new DefaultErrorCode("401", "当前微信凭证不存在"));
        }
        Employee employee = userContext.activeEmployee(credential);
        if (employee == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未加入有效企业"));
        }
        String replacement = required(newPassword, "新密码");
        if (!replacement.equals(confirmPassword)) {
            throw new DefinedException(new DefaultErrorCode("400", "两次输入的新密码不一致"));
        }
        passwordContext.validate(replacement);
        // 安全变更：App 端微信登录已验证身份，密码设置/修改均不要求原密码。
        // 无论是否已有密码哈希，直接设置/覆盖密码；oldPassword 字段保留但不再校验（向后兼容）。
        if (credential.getPasswordHash() == null || credential.getPasswordHash().isBlank()) {
            return passwordAuthContext.setInitialPassword(credential, replacement);
        }
        return passwordAuthContext.changePassword(credential, replacement);
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", label + "不能为空"));
        }
        return value.trim();
    }
}
