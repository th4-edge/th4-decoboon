package com.th4.edge.decoboon.carryover.service.reconciliation.save.support;

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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 双轨对账批次保存实现。 */
@Note("保存双轨对账批次")
@Properties(allow = {
        @AllowProperties(model = CarryoverReconciliationReport.class,
                properties = {"reconciliationId", "batchId", "asOfDate", "periodStart", "periodEnd",
                        "status", "remark", "unclassifiedCount", "pendingCount", "complete", "blockers", "checklist", "items"}),
        @AllowProperties(model = CarryoverReconciliationItem.class,
                properties = {"itemId", "theme", "originalAmount", "originalSource", "originalConfirmed",
                        "systemAmount", "difference", "differenceType", "differenceStatus", "reason", "action",
                        "owner", "dueDate"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.carryover.model.CarryoverChecklistItem.class, properties = {"key", "label", "required", "completed", "exception"})
})
@Service("carryover/reconciliation/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.reconciliation.save.Save {

    @User private Employee employee;
    @AutoInject @NotNull private Long batchId;
    @AutoInject private LocalDate periodStart;
    @AutoInject private LocalDate periodEnd;
    @AutoInject @Length(max = 500) private String remark;

    private final CarryoverReconciliationContext context;

    public Save(CarryoverReconciliationContext context) { this.context = context; }

    @Override
    @Transactional
    public CarryoverReconciliationReport call(CarryoverReconciliation request) {
        return context.save(employee, batchId, periodStart, periodEnd, remark);
    }
}
