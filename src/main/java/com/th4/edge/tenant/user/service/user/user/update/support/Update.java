package com.th4.edge.tenant.user.service.user.user.update.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Note("用户资料更新")
@Properties(allow = {
        @AllowProperties(model = Credentials.class, properties = {"creId", "openId", "unionId", "phone", "status", "lastTime", "employee"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"})
})
@Service("user/user/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.tenant.user.service.user.user.update.Update {
    @AutoInject @Length(max = 15) private String name;
    @AutoInject @Length(max = 512) private String avatarUrl;
    private final UserIdentityContext userContext;
    private final EmployeeRepository employeeRepository;
    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;
    public Update(UserIdentityContext userContext, EmployeeRepository employeeRepository) { this.userContext = userContext; this.employeeRepository = employeeRepository; }
    @Override @Transactional
    public Credentials call(Employee request) {
        Credentials credential = principal instanceof Credentials current
                ? userContext.requireUser(current.getCreId())
                : userContext.credentialFor(employeeRepository.findByEmpId(((Employee) principal).getEmpId()));
        if (credential == null) return null;
        Employee employee = userContext.activeEmployee(credential);
        if (employee != null) {
            if (name != null && !name.isBlank()) employee.setName(name.trim());
            if (avatarUrl != null && !avatarUrl.isBlank()) employee.setAvatarUrl(avatarUrl);
            employeeRepository.save(employee);
            credential.setEmployee(employee);
        }
        return credential;
    }
}
