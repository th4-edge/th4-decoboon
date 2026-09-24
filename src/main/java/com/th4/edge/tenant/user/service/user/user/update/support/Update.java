package com.th4.edge.tenant.user.service.user.user.update.support;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.io.annotation.AutoFile;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.tenant.user.context.AvatarContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.framework.io.StreamFile;
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
    /** 头像文件：头像只能通过该文件字段上传到 OSS，服务端负责回填长期地址。 */
    @AutoFile private StreamFile avatar;
    private final UserIdentityContext userContext;
    private final EmployeeRepository employeeRepository;
    private final AvatarContext avatarContext;
    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;
    public Update(UserIdentityContext userContext, EmployeeRepository employeeRepository, AvatarContext avatarContext) {
        this.userContext = userContext;
        this.employeeRepository = employeeRepository;
        this.avatarContext = avatarContext;
    }
    @Override @Transactional
    public Credentials call(Employee request) {
        Credentials credential = principal instanceof Credentials current
                ? userContext.requireUser(current.getCreId())
                : userContext.credentialFor(employeeRepository.findByEmpId(((Employee) principal).getEmpId()));
        if (credential == null) return null;
        Employee employee = userContext.activeEmployee(credential);
        if (employee != null) {
            if (name != null && !name.isBlank()) employee.setName(name.trim());
            if (avatar != null && !avatar.isEmpty()) {
                AvatarContext.StoredAvatar stored = avatarContext.save(
                        employee.getCompany() == null ? null : employee.getCompany().getComId(),
                        employee.getEmpId(), avatar);
                employee.setAvatarUrl(stored.url());
            } else if (avatarUrl != null) {
                // 显式传空串表示恢复默认头像；非空 URL 必须通过文件上传进入 OSS，禁止客户端直填。
                if (!avatarUrl.isBlank()) {
                    throw new DefinedException(new DefaultErrorCode("400", "头像请通过文件上传"));
                }
                employee.setAvatarUrl(null);
            }
            employeeRepository.save(employee);
            credential.setEmployee(employee);
        }
        return credential;
    }
}
