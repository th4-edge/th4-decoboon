package com.th4.edge.decoboon.carryover.service.reconciliation.report.support;

import com.th4.edge.decoboon.carryover.context.CarryoverReconciliationContext;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationItem;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport;
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
import org.springframework.transaction.annotation.Transactional;

/** 双轨对账查询实现。 */
@Note("双轨对账查询")
@Properties(allow = {
        @AllowProperties(model = CarryoverReconciliationReport.class,
                properties = {"reconciliationId", "batchId", "asOfDate", "periodStart", "periodEnd",
                        "status", "remark", "unclassifiedCount", "pendingCount", "complete", "blockers", "checklist", "items"}),
        @AllowProperties(model = CarryoverReconciliationItem.class,
                properties = {"itemId", "theme", "originalAmount", "originalSource", "originalConfirmed",
                        "systemAmount", "difference", "differenceType", "differenceStatus", "reason", "action",
                        "owner", "dueDate"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/reconciliation/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.carryover.service.reconciliation.report.Report {

    @User private Employee employee;
    @AutoInject @NotNull private Long reconciliationId;
    private final CarryoverReconciliationContext context;

    public Report(CarryoverReconciliationContext context) { this.context = context; }

    @Override
    @Transactional(readOnly = true)
    public CarryoverReconciliationReport call(CarryoverReconciliation request) {
        return context.report(employee, reconciliationId);
    }
}
