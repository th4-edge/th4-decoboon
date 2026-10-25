package com.th4.edge.decoboon.finance.service.payments.employee.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Note("员工待发应付分页")
@Properties(allow = {
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "project", "name", "type", "payeeEmployee", "payeeName", "period",
                        "amount", "paidAmount", "dueDate", "status", "sourceType", "sourceId", "remark"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/payment/employee/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.payments.employee.page.Page {
    @User private Employee employee;
    @AutoInject private Long empId;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private Long storeId;
    private final PayableRepository repository;
    private final FinanceAccessContext accessContext;
    private final PayrollAccessContext payrollAccessContext;
    private final StoreFilterContext storeFilterContext;
    public Page(PayableRepository repository, FinanceAccessContext accessContext,
                PayrollAccessContext payrollAccessContext, StoreFilterContext storeFilterContext) {
        this.repository = repository; this.accessContext = accessContext;
        this.payrollAccessContext = payrollAccessContext; this.storeFilterContext = storeFilterContext;
    }
    @Override
    public org.springframework.data.domain.Page<Payable> call(Payable request) {
        // DEC-093：店长不得读取员工工资/提成待发池或执行发放。
        Employee currentEmployee = payrollAccessContext.requireEditor(employee);
        Long storeFilter = storeFilterContext.resolve(currentEmployee, storeId);
        var items = repository.findAll(accessContext.visible(currentEmployee, Payable.class)).stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> item.getType() == PayableType.EmployeeSalary
                        || item.getType() == PayableType.EmployeeCommission)
                .filter(item -> item.getAmount() != null
                        && item.getAmount().subtract(item.getPaidAmount() == null ? BigDecimal.ZERO : item.getPaidAmount()).signum() > 0)
                .filter(item -> empId == null || (item.getPayeeEmployee() != null
                        && empId.equals(item.getPayeeEmployee().getEmpId())))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
