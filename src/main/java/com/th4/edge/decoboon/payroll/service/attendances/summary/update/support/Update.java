package com.th4.edge.decoboon.payroll.service.attendances.summary.update.support;

import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
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
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    @AutoInject private Integer normalDays;
    @AutoInject private Integer personalLeaveDays;
    @AutoInject private Integer sickLeaveDays;
    @AutoInject private Integer absenceDays;
    @AutoInject private Integer externalDays;
    @AutoInject private Integer annualLeaveDays;
    @AutoInject private Integer compLeaveDays;
    @AutoInject private String status;
    @AutoInject private String remark;
    private final AttendanceSummaryRepository repository;
    private final PayrollAccessContext accessContext;
    public Update(AttendanceSummaryRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository; this.accessContext = accessContext;
    }
    @Override @Transactional
    public AttendanceSummary call(AttendanceSummary request) {
        Employee current = accessContext.requireManager(employee);
        AttendanceSummary summary = repository.findById(summaryId)
                .filter(item -> item.getCompany() != null
                        && current.getCompany().getComId().equals(item.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "考勤不存在或无权访问")));
        if (current.getRole() == com.th4.edge.tenant.user.configure.EmployeeRole.StoreManager
                && (summary.getStore() == null || current.getStore() == null
                || !summary.getStore().getStoreId().equals(current.getStore().getStoreId()))) {
            throw new DefinedException(new DefaultErrorCode("404", "考勤不存在或无权访问"));
        }
        if (normalDays != null) summary.setNormalDays(normalDays);
        if (personalLeaveDays != null) summary.setPersonalLeaveDays(personalLeaveDays);
        if (sickLeaveDays != null) summary.setSickLeaveDays(sickLeaveDays);
        if (absenceDays != null) summary.setAbsenceDays(absenceDays);
        if (externalDays != null) summary.setExternalDays(externalDays);
        if (annualLeaveDays != null) summary.setAnnualLeaveDays(annualLeaveDays);
        if (compLeaveDays != null) summary.setCompLeaveDays(compLeaveDays);
        if (remark != null) summary.setRemark(remark);
        AttendanceStatus nextStatus = parseStatus();
        if (nextStatus != null) {
            summary.setStatus(nextStatus);
            if (nextStatus == AttendanceStatus.Confirmed) {
                summary.setConfirmedBy(current);
                summary.setConfirmedAt(LocalDateTime.now());
            }
        }
        return repository.save(summary);
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
