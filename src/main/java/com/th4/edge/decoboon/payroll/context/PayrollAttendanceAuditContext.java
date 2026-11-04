package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.tenant.user.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 月度考勤结构化审计：不新增数据库表，保留操作人、对象、前后值和动作。 */
@Component
public class PayrollAttendanceAuditContext {

    private static final Logger LOG = LoggerFactory.getLogger(PayrollAttendanceAuditContext.class);

    public void record(Employee operator, AttendanceSummary attendance, String action,
                       String beforeSnapshot, String reason) {
        if (operator == null || attendance == null) return;
        LOG.info("payroll_attendance_audit action={} operatorEmpId={} comId={} storeId={} summaryId={} employeeEmpId={} payrollMonth={} before={} after={} reason={}",
                action,
                operator.getEmpId(),
                operator.getCompany() == null ? null : operator.getCompany().getComId(),
                operator.getStore() == null ? null : operator.getStore().getStoreId(),
                attendance.getSummaryId(),
                attendance.getEmployee() == null ? null : attendance.getEmployee().getEmpId(),
                attendance.getPayrollMonth(),
                beforeSnapshot,
                snapshot(attendance),
                reason);
    }

    public String snapshot(AttendanceSummary attendance) {
        if (attendance == null) return "-";
        return "scheduledDays=" + value(attendance.getScheduledDays())
                + ",normalDays=" + value(attendance.getNormalDays())
                + ",personalLeaveDays=" + value(attendance.getPersonalLeaveDays())
                + ",sickLeaveDays=" + value(attendance.getSickLeaveDays())
                + ",absenceDays=" + value(attendance.getAbsenceDays())
                + ",externalDays=" + value(attendance.getExternalDays())
                + ",annualLeaveDays=" + value(attendance.getAnnualLeaveDays())
                + ",compLeaveDays=" + value(attendance.getCompLeaveDays())
                + ",status=" + attendance.getStatus()
                + ",confirmedBy=" + (attendance.getConfirmedBy() == null ? null : attendance.getConfirmedBy().getEmpId())
                + ",confirmedAt=" + attendance.getConfirmedAt()
                + ",remark=" + attendance.getRemark();
    }

    private BigDecimal value(Integer value) {
        return BigDecimal.valueOf(value == null ? 0 : value);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
