package com.th4.edge.tenant.user.service.auth.session.passwordlogout.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.PasswordAuthContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("退出登录")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"})
})
@Service("auth/session/logout")
@Scope(ScopeTypes.PROTOTYPE)
public class PasswordLogout implements com.th4.edge.tenant.user.service.auth.session.passwordlogout.PasswordLogout {

    @User
    private Employee employee;

    private final PasswordAuthContext passwordAuthContext;
    private final UserIdentityContext userContext;

    public PasswordLogout(PasswordAuthContext passwordAuthContext, UserIdentityContext userContext) {
        this.passwordAuthContext = passwordAuthContext;
        this.userContext = userContext;
    }

    @Override
    @Transactional
    public Employee call(Employee request) {
        Credentials credential = userContext.requireWx(employee);
        passwordAuthContext.logout(credential);
        return employee;
    }
}
