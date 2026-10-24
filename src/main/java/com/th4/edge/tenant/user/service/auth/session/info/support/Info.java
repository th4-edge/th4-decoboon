package com.th4.edge.tenant.user.service.auth.session.info.support;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.configure.EmployeeRolePolicy;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
@Note("当前会话身份")
@Properties(allow = {
        @AllowProperties(model = Credentials.class,
                properties = {"creId", "phone", "status", "lastTime", "passwordSet", "mustChangePassword", "employee", "sessionType", "customerAccesses"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "resignedAt", "resignReason", "createTime"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color", "addr", "setting"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"}),
        @AllowProperties(model = CustomerAccess.class,
                properties = {"accessId", "displayName", "status", "grantedAt", "project", "customer"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "area", "status", "progress", "property", "customer"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community", "addr"})
})
@Service("auth/session/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.tenant.user.service.auth.session.info.Info {
    @com.th4.edge.framework.provider.security.annotation.User
    private com.th4.edge.framework.provider.security.User<?> principal;
    private final UserIdentityContext userContext;
    private final CustomerAccessContext customerAccessContext;
    private final EmployeeRepository employeeRepository;
    public Info(UserIdentityContext userContext, CustomerAccessContext customerAccessContext,
                EmployeeRepository employeeRepository) {
        this.userContext = userContext;
        this.customerAccessContext = customerAccessContext;
        this.employeeRepository = employeeRepository;
    }
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
        EmployeeRolePolicy.requireApplicationAccess(credential.getEmployee());
        customerAccessContext.attachSession(credential);
        return credential;
    }
}
