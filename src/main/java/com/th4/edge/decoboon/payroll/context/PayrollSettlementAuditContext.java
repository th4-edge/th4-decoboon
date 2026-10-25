package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.tenant.user.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 工资单状态动作审计：不新增数据库表，按结构化日志保留操作人、前后状态与原因。 */
@Component
public class PayrollSettlementAuditContext {

    private static final Logger LOG = LoggerFactory.getLogger(PayrollSettlementAuditContext.class);

    public void record(Employee operator, PayrollSheet sheet, String action,
                       PayrollSheetStatus beforeStatus, PayrollSheetStatus afterStatus, String reason) {
        if (operator == null || sheet == null) return;
        LOG.info("payroll_sheet_audit action={} operatorEmpId={} comId={} storeId={} sheetId={} period={} before={} after={} gross={} deduction={} net={} reason={}",
                action,
                operator.getEmpId(),
                operator.getCompany() == null ? null : operator.getCompany().getComId(),
                operator.getStore() == null ? null : operator.getStore().getStoreId(),
                sheet.getSheetId(),
                sheet.getPeriod(),
                beforeStatus,
                afterStatus,
                sheet.getTotalGrossAmount(),
                sheet.getTotalDeductionAmount(),
                sheet.getTotalNetAmount(),
                reason);
    }
}
