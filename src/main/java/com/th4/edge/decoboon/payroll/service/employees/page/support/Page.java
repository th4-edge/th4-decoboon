package com.th4.edge.decoboon.payroll.service.employees.page.support;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;

/** 薪酬域员工选择器：BOSS、财务可按企业范围选择发薪员工。 */
@Note("薪酬员工选择器")
@Properties(allow = {
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role", "status", "store", "team"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Team.class, properties = {"teamId", "name"})
})
@Service("payroll/employee/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.payroll.service.employees.page.Page {

    @User private Employee employee;
    @AutoInject private String keyword;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    private final EmployeeRepository repository;
    private final PayrollAccessContext accessContext;

    public Page(EmployeeRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Employee> call(Employee request) {
        Employee currentEmployee = accessContext.requireEditor(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        var items = repository.findByCompany_ComId(currentEmployee.getCompany().getComId()).stream()
                .filter(item -> kw.isBlank()
                        || (item.getName() != null && item.getName().toLowerCase().contains(kw))
                        || (item.getTelNo() != null && item.getTelNo().contains(kw)))
                .sorted(Comparator.comparing(Employee::getName,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 50 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
