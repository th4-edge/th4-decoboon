package com.th4.edge.decoboon.payroll.service.attendances.summary.delete.support;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceAuditContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 删除尚未进入工资单的月度考勤。 */
@Note("月度考勤删除")
@Properties(allow = {
        @AllowProperties(model = AttendanceSummary.class,
                properties = {"summaryId", "employee", "month", "scheduledDays", "normalDays", "personalLeaveDays", "sickLeaveDays",
                        "absenceDays", "externalDays", "annualLeaveDays", "compLeaveDays", "status", "remark"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("attendance/summary/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.payroll.service.attendances.summary.delete.Delete {

    @User private Employee employee;
    @AutoInject @NotNull private Long summaryId;

    private final AttendanceSummaryRepository repository;
    private final PayrollItemRepository payrollItemRepository;
    private final PayrollAccessContext accessContext;
    private final PayrollAttendanceAuditContext auditContext;

    public Delete(AttendanceSummaryRepository repository,
                  PayrollItemRepository payrollItemRepository,
                  PayrollAccessContext accessContext,
                  PayrollAttendanceAuditContext auditContext) {
        this.repository = repository;
        this.payrollItemRepository = payrollItemRepository;
        this.accessContext = accessContext;
        this.auditContext = auditContext;
    }

    @Override
    @Transactional
    public AttendanceSummary call(AttendanceSummary request) {
        Employee current = accessContext.requireManager(employee);
        Specification<AttendanceSummary> byId = (root, query, builder) -> builder.equal(root.get("summaryId"), summaryId);
        AttendanceSummary summary = repository.findOne(byId.and(accessContext.visible(current, AttendanceSummary.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "考勤不存在或无权访问")));
        if (summary.getStatus() == AttendanceStatus.InPayroll) {
            throw new DefinedException(new DefaultErrorCode("409", "考勤已进入工资单，不能删除"));
        }
        Long comId = current.getCompany().getComId();
        Long empId = summary.getEmployee().getEmpId();
        if (payrollItemRepository.existsByCompany_ComIdAndEmployee_EmpIdAndSheet_Period(
                comId, empId, summary.getPayrollMonth())) {
            throw new DefinedException(new DefaultErrorCode(
                    "409", "该员工当月工资项已生成，不能删除考勤；请先在员工工资处理同月工资单，草稿可删除，待确认工资单需由 BOSS/店长退回草稿"));
        }
        String beforeSnapshot = auditContext.snapshot(summary);
        repository.delete(summary);
        auditContext.record(current, summary, "DELETE", beforeSnapshot, "删除未进入工资单的月度考勤");
        return summary;
    }
}
