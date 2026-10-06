package com.th4.edge.decoboon.payroll.service.commissionadjustments.save.support;

import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentType;
import com.th4.edge.decoboon.payroll.context.CommissionAdjustmentContext;
import com.th4.edge.decoboon.payroll.model.CommissionAdjustment;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 新增退款、离职或跨期负向调整。 */
@Note("提成负向调整新增")
@Properties(allow = {
        @AllowProperties(model = CommissionAdjustment.class,
                properties = {"adjustmentId", "employee", "project", "originalCommission", "type", "status",
                        "refundAmount", "deductionAmount", "appliedAmount", "occurredAt", "remark", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.payroll.model.CommissionItem.class,
                properties = {"commissionId", "contractAmount", "commissionAmount", "releasedAmount", "paidAmount", "status"})
})
@Service("payroll/commission/adjustment/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.payroll.service.commissionadjustments.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long originalCommissionId;
    @AutoInject
    @NotNull
    private CommissionAdjustmentType type;
    @AutoInject
    private BigDecimal refundAmount;
    @AutoInject
    private BigDecimal deductionAmount;
    @AutoInject
    private String idempotencyKey;
    @AutoInject
    private LocalDate occurredAt;
    @AutoInject
    private String remark;

    private final CommissionAdjustmentContext adjustmentContext;

    public Save(CommissionAdjustmentContext adjustmentContext) {
        this.adjustmentContext = adjustmentContext;
    }

    @Override
    public CommissionAdjustment call(CommissionAdjustment request) {
        return adjustmentContext.create(employee, originalCommissionId, type, refundAmount,
                deductionAmount, idempotencyKey, occurredAt, remark);
    }
}
