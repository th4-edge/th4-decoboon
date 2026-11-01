package com.th4.edge.decoboon.payroll.service.employees.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/** 薪酬域员工选择器：BOSS、财务可按企业范围选择发薪员工，店长限本店。 */
@Note("薪酬员工选择器")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Team.class, properties = {"teamId", "name"})
})
@Service("payroll/employee/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.payroll.service.employees.page.Page {

    @User private Employee employee;
    @AutoInject private String keyword;
    /** 支持单角色或逗号分隔多角色，如 Boss,Pm。 */
    @AutoInject private String role;
    @AutoInject private Long storeId;
    @AutoInject private String status;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    /** 已选员工按 ID 回读，仍受薪酬权限与门店范围约束。 */
    @AutoInject private Long empId;
    /** 离职交接等场景排除当前对象本人。 */
    @AutoInject private Long excludeEmpId;
    private final EmployeeRepository repository;
    private final PayrollAccessContext accessContext;
    @org.springframework.beans.factory.annotation.Autowired
    private StoreFilterContext storeFilterContext;

    public Page(EmployeeRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Employee> call(Employee request) {
        Employee currentEmployee = accessContext.requireManager(employee);
        Long storeFilter = storeFilterContext.resolve(currentEmployee, storeId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String keywordPattern = normalizedKeyword.isBlank() ? null : "%" + normalizedKeyword + "%";
        return repository.findPickerPage(
                currentEmployee.getCompany().getComId(),
                storeFilter == null || storeFilter == 0L ? null : storeFilter,
                roleFilter(role),
                statusFilter(status),
                empId,
                excludeEmpId,
                keywordPattern,
                PageRequest.of(page, pageSize));
    }

    private List<EmployeeRole> roleFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return Arrays.asList(EmployeeRole.values());
        }
        try {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .map(EmployeeRole::valueOf)
                    .distinct()
                    .toList();
        } catch (IllegalArgumentException ignored) {
            throw new DefinedException(new DefaultErrorCode("400", "员工岗位筛选值无效"));
        }
    }

    private List<EmployeeStatus> statusFilter(String value) {
        if (value == null || value.isBlank()) {
            return List.of(EmployeeStatus.Active);
        }
        if ("all".equalsIgnoreCase(value.trim())) {
            return Arrays.asList(EmployeeStatus.values());
        }
        try {
            return List.of(EmployeeStatus.valueOf(value.trim()));
        } catch (IllegalArgumentException ignored) {
            throw new DefinedException(new DefaultErrorCode("400", "员工状态筛选值无效"));
        }
    }
}
