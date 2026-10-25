package com.th4.edge.tenant.user.service.user.employee.page.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.service.annotation.ServiceName;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 当前企业员工分页。
 */
@Note("员工分页")
@Description("仅查询当前登录员工所属企业的成员")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.tenant.user.model.Credentials.class,
                properties = {"creId", "openId", "unionId", "phone", "status", "employee"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "telNo", "role", "store", "team", "company", "status", "avatarUrl", "joinedAt", "createTime"}),
        @AllowProperties(model = Company.class,
                properties = {"comId", "title", "name", "logo", "color", "size", "addr", "setting", "createTime"}),
        @AllowProperties(model = Store.class,
                properties = {"storeId", "name", "defaultStore", "color", "addr", "company", "createTime"}),
        @AllowProperties(model = Team.class,
                properties = {"teamId", "name", "color", "store", "company", "createTime"})
})
@Service("user/employee/page")
@ServiceName("page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.tenant.user.service.user.employee.page.Page {

    /** 框架注入：当前登录员工。 */
    @User
    private Employee employee;

    private final TenantContext tenantContext;
    private final EmployeeRepository employeeRepository;

    /** 当前页，前端从 1 开始。 */
    @AutoInject
    private Integer current;

    /** 每页数量。 */
    @AutoInject
    private Integer size;

    public Page(TenantContext tenantContext, EmployeeRepository employeeRepository) {
        this.tenantContext = tenantContext;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public org.springframework.data.domain.Page<Employee> call(Employee request) {
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 500);
        Employee currentEmployee = tenantContext.requireRole(employee,
                EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.Designer, EmployeeRole.Pm);
        PageRequest pageable = PageRequest.of(page, pageSize);
        if (currentEmployee.getRole() == EmployeeRole.StoreManager) {
            if (currentEmployee.getStore() == null || currentEmployee.getStore().getStoreId() == null) {
                throw new com.th4.edge.framework.exception.DefinedException(
                        new com.th4.edge.framework.exception.DefaultErrorCode("403", "当前店长未归属门店"));
            }
            return employeeRepository.findByCompany_ComIdAndStore_StoreId(
                    currentEmployee.getCompany().getComId(),
                    currentEmployee.getStore().getStoreId(),
                    pageable);
        }
        return employeeRepository.findByCompany_ComId(
                currentEmployee.getCompany().getComId(), pageable);
    }
}
