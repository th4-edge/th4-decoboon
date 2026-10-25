package com.th4.edge.decoboon.payroll.service.attendances.summary.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Note("月度考勤分页")
@Properties(allow = {
        @AllowProperties(model = AttendanceSummary.class,
                properties = {"summaryId", "employee", "month", "scheduledDays", "normalDays", "personalLeaveDays", "sickLeaveDays",
                        "absenceDays", "externalDays", "annualLeaveDays", "compLeaveDays", "status", "confirmedBy", "confirmedAt", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("attendance/summary/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.payroll.service.attendances.summary.page.Page {
    @User private Employee employee;
    @AutoInject private String month;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private Long storeId;
    private final AttendanceSummaryRepository repository;
    private final PayrollAccessContext accessContext;
    private final StoreFilterContext storeFilterContext;
    public Page(AttendanceSummaryRepository repository, PayrollAccessContext accessContext,
                StoreFilterContext storeFilterContext) {
        this.repository = repository; this.accessContext = accessContext; this.storeFilterContext = storeFilterContext;
    }
    @Override
    public org.springframework.data.domain.Page<AttendanceSummary> call(AttendanceSummary request) {
        Employee currentEmployee = accessContext.requireManager(employee);
        Long storeFilter = storeFilterContext.resolve(currentEmployee, storeId);
        var items = repository.findAll(accessContext.visible(currentEmployee, AttendanceSummary.class))
                .stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> month == null || month.isBlank() || month.equals(item.getPayrollMonth()))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
