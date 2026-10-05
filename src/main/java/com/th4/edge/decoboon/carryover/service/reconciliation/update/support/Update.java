package com.th4.edge.decoboon.carryover.service.reconciliation.update.support;

import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationStatus;
import com.th4.edge.decoboon.carryover.context.CarryoverReconciliationContext;
import com.th4.edge.decoboon.carryover.model.CarryoverChecklistItem;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationItem;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 双轨对账状态流转实现。 */
@Note("更新双轨对账状态")
@Properties(allow = {
        @AllowProperties(model = CarryoverReconciliationReport.class,
                properties = {"reconciliationId", "batchId", "asOfDate", "periodStart", "periodEnd",
                        "status", "remark", "unclassifiedCount", "pendingCount", "complete", "blockers", "checklist", "items"}),
        @AllowProperties(model = CarryoverChecklistItem.class,
                properties = {"key", "label", "required", "completed", "exception"}),
        @AllowProperties(model = CarryoverReconciliationItem.class,
                properties = {"itemId", "theme", "originalAmount", "originalSource", "originalConfirmed",
                        "systemAmount", "difference", "differenceType", "differenceStatus", "reason", "action",
                        "owner", "dueDate"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/reconciliation/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.carryover.service.reconciliation.update.Update {

    @User private Employee employee;
    @AutoInject @NotNull private Long reconciliationId;
    @AutoInject @NotNull private CarryoverReconciliationStatus status;
    @AutoInject @Length(max = 500) private String remark;
    private final CarryoverReconciliationContext context;

    public Update(CarryoverReconciliationContext context) { this.context = context; }

    @Override
    @Transactional
    public CarryoverReconciliationReport call(CarryoverReconciliation request) {
        return context.updateStatus(employee, reconciliationId, status, remark);
    }
}
