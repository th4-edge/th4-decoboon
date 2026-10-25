package com.th4.edge.decoboon.payroll.service.sheets.update.support;

import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.context.PayrollSettlementContext;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 工资单复核提交、BOSS 退回和最终确认。 */
@Note("工资单复核与确认")
@Properties(allow = {
        @AllowProperties(model = PayrollSheet.class,
                properties = {"sheetId", "period", "status", "attendanceComplete", "commissionComplete", "totalGrossAmount",
                "totalDeductionAmount", "totalNetAmount", "confirmedBy", "confirmedAt", "remark", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("payroll/sheet/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.payroll.service.sheets.update.Update {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long sheetId;

    @AutoInject
    private PayrollSheetStatus status;

    @AutoInject
    @Length(max = 480)
    private String reason;

    private final PayrollSettlementContext settlementContext;

    public Update(PayrollSettlementContext settlementContext) {
        this.settlementContext = settlementContext;
    }

    @Override
    public PayrollSheet call(PayrollSheet request) {
        if (status == null || status == PayrollSheetStatus.Confirmed) {
            return settlementContext.confirm(employee, sheetId);
        }
        if (status == PayrollSheetStatus.Pending) {
            return settlementContext.submitReview(employee, sheetId);
        }
        if (status == PayrollSheetStatus.Draft) {
            return settlementContext.returnToDraft(employee, sheetId, reason);
        }
        throw new DefinedException(new DefaultErrorCode("400", "不支持的工资单状态变更"));
    }
}
