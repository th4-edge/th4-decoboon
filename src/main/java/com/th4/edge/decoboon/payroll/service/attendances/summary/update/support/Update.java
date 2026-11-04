package com.th4.edge.decoboon.payroll.service.attendances.summary.update.support;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceAuditContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceValidationContext;
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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Note("月度考勤确认/更新")
@Properties(allow = {
        @AllowProperties(model = AttendanceSummary.class,
                properties = {"summaryId", "employee", "month", "scheduledDays", "normalDays", "personalLeaveDays", "sickLeaveDays",
                        "absenceDays", "externalDays", "annualLeaveDays", "compLeaveDays", "status", "confirmedBy", "confirmedAt", "remark", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("attendance/summary/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.payroll.service.attendances.summary.update.Update {
    @User private Employee employee;
    @AutoInject @NotNull private Long summaryId;
    @AutoInject private BigDecimal scheduledDays;
    @AutoInject private BigDecimal normalDays;
    @AutoInject private BigDecimal personalLeaveDays;
    @AutoInject private BigDecimal sickLeaveDays;
    @AutoInject private BigDecimal absenceDays;
    @AutoInject private BigDecimal externalDays;
    @AutoInject private BigDecimal annualLeaveDays;
    @AutoInject private BigDecimal compLeaveDays;
    @AutoInject private String status;
    @AutoInject @Length(max = 500) private String remark;
    private final AttendanceSummaryRepository repository;
    private final PayrollItemRepository payrollItemRepository;
    private final PayrollAccessContext accessContext;
    private final PayrollAttendanceValidationContext validationContext;
    private final PayrollAttendanceAuditContext auditContext;
    public Update(AttendanceSummaryRepository repository, PayrollItemRepository payrollItemRepository,
                  PayrollAccessContext accessContext,
                  PayrollAttendanceValidationContext validationContext,
                  PayrollAttendanceAuditContext auditContext) {
        this.repository = repository;
        this.payrollItemRepository = payrollItemRepository;
        this.accessContext = accessContext;
        this.validationContext = validationContext;
        this.auditContext = auditContext;
    }
    @Override @Transactional
    public AttendanceSummary call(AttendanceSummary request) {
        Employee current = accessContext.requireManager(employee);
        Specification<AttendanceSummary> byId =
                (root, query, builder) -> builder.equal(root.get("summaryId"), summaryId);
        AttendanceSummary summary = repository.findOne(
                        byId.and(accessContext.visible(current, AttendanceSummary.class)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "考勤不存在或无权访问")));
        if (summary.getStatus() == AttendanceStatus.InPayroll) {
            throw new DefinedException(new DefaultErrorCode(
                    "409", "考勤已进入工资单，不能修改；请先删除同月草稿工资单，待确认工资单需由 BOSS/店长退回草稿"));
        }
        if (payrollItemRepository.existsByCompany_ComIdAndEmployee_EmpIdAndSheet_Period(
                current.getCompany().getComId(), summary.getEmployee().getEmpId(), summary.getPayrollMonth())) {
            throw new DefinedException(new DefaultErrorCode(
                    "409", "该员工当月工资项已生成，不能修改考勤；请先在员工工资处理同月工资单，草稿可删除，待确认工资单需由 BOSS/店长退回草稿"));
        }

        String beforeSnapshot = auditContext.snapshot(summary);
        AttendanceStatus targetStatus = parseStatus();
        if (targetStatus != null && targetStatus != AttendanceStatus.Confirmed) {
            throw new DefinedException(new DefaultErrorCode("400", "考勤更新接口仅支持确认动作"));
        }
        boolean dataChanged = applyFields(summary);

        if (targetStatus == AttendanceStatus.Confirmed) {
            if (summary.getStatus() == AttendanceStatus.Confirmed) {
                if (dataChanged) {
                    throw new DefinedException(new DefaultErrorCode(
                            "409", "已确认考勤不能同时修改和重复确认，请先提交数据变更，再重新确认"));
                }
                return summary;
            }
            if (summary.getStatus() != AttendanceStatus.Draft && summary.getStatus() != AttendanceStatus.Pending) {
                throw new DefinedException(new DefaultErrorCode("409", "当前考勤状态不能直接确认"));
            }
            validationContext.requireComplete(summary);
            summary.setStatus(AttendanceStatus.Confirmed);
            summary.setConfirmedBy(current);
            summary.setConfirmedAt(LocalDateTime.now());
            AttendanceSummary saved = repository.saveAndFlush(summary);
            auditContext.record(current, saved, "CONFIRM", beforeSnapshot, null);
            return saved;
        }

        validationContext.validate(summary);
        boolean confirmationReset = dataChanged && summary.getStatus() == AttendanceStatus.Confirmed;
        if (confirmationReset) {
            summary.setStatus(AttendanceStatus.Pending);
            summary.setConfirmedBy(null);
            summary.setConfirmedAt(null);
        }
        AttendanceSummary saved = repository.saveAndFlush(summary);
        auditContext.record(current, saved,
                confirmationReset ? "UPDATE_RESET_CONFIRMATION" : "UPDATE",
                beforeSnapshot,
                confirmationReset ? "已确认数据发生变更，退回待确认" : null);
        return saved;
    }

    private boolean applyFields(AttendanceSummary summary) {
        boolean changed = false;
        if (scheduledDays != null) {
            Integer nextScheduledDays = validationContext.integerScheduledDays(scheduledDays);
            if (!Objects.equals(nextScheduledDays, summary.getScheduledDays())) {
                summary.setScheduledDays(nextScheduledDays);
                changed = true;
            }
        }
        if (normalDays != null && !Objects.equals(normalDays, summary.getNormalDays())) {
            summary.setNormalDays(normalDays);
            changed = true;
        }
        if (personalLeaveDays != null && !Objects.equals(personalLeaveDays, summary.getPersonalLeaveDays())) {
            summary.setPersonalLeaveDays(personalLeaveDays);
            changed = true;
        }
        if (sickLeaveDays != null && !Objects.equals(sickLeaveDays, summary.getSickLeaveDays())) {
            summary.setSickLeaveDays(sickLeaveDays);
            changed = true;
        }
        if (absenceDays != null && !Objects.equals(absenceDays, summary.getAbsenceDays())) {
            summary.setAbsenceDays(absenceDays);
            changed = true;
        }
        if (externalDays != null && !Objects.equals(externalDays, summary.getExternalDays())) {
            summary.setExternalDays(externalDays);
            changed = true;
        }
        if (annualLeaveDays != null && !Objects.equals(annualLeaveDays, summary.getAnnualLeaveDays())) {
            summary.setAnnualLeaveDays(annualLeaveDays);
            changed = true;
        }
        if (compLeaveDays != null && !Objects.equals(compLeaveDays, summary.getCompLeaveDays())) {
            summary.setCompLeaveDays(compLeaveDays);
            changed = true;
        }
        if (remark != null && !Objects.equals(remark, summary.getRemark())) {
            summary.setRemark(remark);
            changed = true;
        }
        return changed;
    }

    private AttendanceStatus parseStatus() {
        if (status == null || status.isBlank()) return null;
        try {
            return AttendanceStatus.valueOf(status.trim());
        } catch (IllegalArgumentException ignored) {
            throw new DefinedException(new DefaultErrorCode("400", "考勤状态值无效"));
        }
    }
}
