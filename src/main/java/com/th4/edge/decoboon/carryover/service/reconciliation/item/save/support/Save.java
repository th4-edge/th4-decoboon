package com.th4.edge.decoboon.carryover.service.reconciliation.item.save.support;

import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverDifferenceType;
import com.th4.edge.decoboon.carryover.context.CarryoverReconciliationContext;
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

import java.math.BigDecimal;
import java.time.LocalDate;

/** 双轨对账主题保存实现。 */
@Note("保存双轨对账主题")
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
@Service("carryover/reconciliation/item/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.reconciliation.item.save.Save {

    @User private Employee employee;
    @AutoInject @NotNull private Long itemId;
    @AutoInject private BigDecimal originalAmount;
    @AutoInject @Length(max = 500) private String originalSource;
    @AutoInject private Boolean originalConfirmed;
    @AutoInject private CarryoverDifferenceType differenceType;
    @AutoInject private CarryoverDifferenceStatus differenceStatus;
    @AutoInject @Length(max = 500) private String reason;
    @AutoInject @Length(max = 500) private String action;
    @AutoInject private Long ownerId;
    @AutoInject private LocalDate dueDate;

    private final CarryoverReconciliationContext context;

    public Save(CarryoverReconciliationContext context) { this.context = context; }

    @Override
    @Transactional
    public CarryoverReconciliationReport call(CarryoverReconciliationItem request) {
        return context.saveItem(employee, itemId, originalAmount, originalSource, originalConfirmed,
                differenceType, differenceStatus, reason, action, ownerId, dueDate);
    }
}
