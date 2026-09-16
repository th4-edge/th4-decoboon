package com.th4.edge.tenant.user.service.user.user.info.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
@Note("用户信息")
@Properties(allow = {
        @AllowProperties(model = Credentials.class, properties = {"creId", "openId", "unionId", "phone", "status", "lastTime", "mustChangePassword", "employee"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"})
})
@Service("user/user/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.tenant.user.service.user.user.info.Info {
    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;
    private final UserIdentityContext userContext;
    private final EmployeeRepository employeeRepository;
    public Info(UserIdentityContext userContext, EmployeeRepository employeeRepository) { this.userContext = userContext; this.employeeRepository = employeeRepository; }
    @Override
    public Credentials call(Employee request) {
        Credentials credential;
        if (principal instanceof Employee employeeToken) {
            Employee employee = employeeRepository.findByEmpId(employeeToken.getEmpId());
            credential = userContext.credentialFor(employee);
        } else {
            credential = userContext.requireUser(Long.valueOf(principal.getUserId().toString()));
        }
        if (credential == null) throw new DefinedException(new DefaultErrorCode("401", "当前微信凭证不存在"));
        credential.setEmployee(userContext.activeEmployee(credential));
        return credential;
    }
}
