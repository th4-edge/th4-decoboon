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
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private static final int MAX_PAGE_SIZE = 50;

    @User private Employee employee;
    @AutoInject private String month;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private Long storeId;
    /** 本人视角：管理角色也可显式限定为登录员工本人，客户端不能借此指定他人。 */
    @AutoInject private Boolean selfOnly;
    private final AttendanceSummaryRepository repository;
    private final PayrollAccessContext accessContext;
    private final StoreFilterContext storeFilterContext;
    public Page(AttendanceSummaryRepository repository, PayrollAccessContext accessContext,
                StoreFilterContext storeFilterContext) {
        this.repository = repository; this.accessContext = accessContext; this.storeFilterContext = storeFilterContext;
    }
    @Override
    public org.springframework.data.domain.Page<AttendanceSummary> call(AttendanceSummary request) {
        Employee currentEmployee = accessContext.requireAttendanceViewer(employee);
        Specification<AttendanceSummary> specification =
                accessContext.visible(currentEmployee, AttendanceSummary.class);
        if (Boolean.TRUE.equals(selfOnly) || !accessContext.isPayrollManager(currentEmployee)) {
            Long currentEmpId = currentEmployee.getEmpId();
            specification = specification.and((root, query, cb) -> cb.equal(root.get("employee").get("empId"), currentEmpId));
        } else {
            Long storeFilter = storeFilterContext.resolve(currentEmployee, storeId);
            if (!StoreScope.isAllStores(storeFilter)) {
                specification = specification.and((root, query, cb) -> cb.equal(root.get("store").get("storeId"), storeFilter));
            }
        }
        String normalizedMonth = month == null ? "" : month.trim();
        if (!normalizedMonth.isBlank()) {
            if (!normalizedMonth.matches("\\d{4}-(0[1-9]|1[0-2])")) {
                throw new DefinedException(new DefaultErrorCode("400", "工资月份格式必须为 YYYY-MM"));
            }
            specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("payrollMonth"), normalizedMonth));
        }
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        Sort sort = Sort.by(
                Sort.Order.asc("employee.name"),
                Sort.Order.desc("payrollMonth"),
                Sort.Order.asc("summaryId"));
        return repository.findAll(specification, PageRequest.of(page, pageSize, sort));
    }
}
