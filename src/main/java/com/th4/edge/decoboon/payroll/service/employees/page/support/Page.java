package com.th4.edge.decoboon.payroll.service.employees.page.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

/** 薪酬域员工选择器：BOSS、财务可按企业范围选择发薪员工。 */
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
    @AutoInject private String role;
    @AutoInject private Long storeId;
    @AutoInject private String status;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final EmployeeRepository repository;
    private final PayrollAccessContext accessContext;

    public Page(EmployeeRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Employee> call(Employee request) {
        Employee currentEmployee = accessContext.requireEditor(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        EmployeeRole roleFilter = roleFilter(role);
        EmployeeStatus statusFilter = statusFilter(status);
        var source = statusFilter == null
                ? repository.findByCompany_ComIdOrderByNameAsc(currentEmployee.getCompany().getComId())
                : repository.findByCompany_ComIdAndStatusOrderByNameAsc(currentEmployee.getCompany().getComId(), statusFilter);
        var items = source.stream()
                .filter(item -> roleFilter == null || item.getRole() == roleFilter)
                .filter(item -> storeId == null
                        || (item.getStore() != null && storeId.equals(item.getStore().getStoreId())))
                .filter(item -> kw.isBlank()
                        || (item.getName() != null && item.getName().toLowerCase().contains(kw))
                        || (item.getTelNo() != null && item.getTelNo().contains(kw)))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 50 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }

    private EmployeeRole roleFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) return null;
        try {
            return EmployeeRole.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("400", "员工岗位筛选值无效"));
        }
    }

    private EmployeeStatus statusFilter(String value) {
        if (value == null || value.isBlank()) return EmployeeStatus.Active;
        if ("all".equalsIgnoreCase(value)) return null;
        try {
            return EmployeeStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("400", "员工状态筛选值无效"));
        }
    }
}
