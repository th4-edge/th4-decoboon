package com.th4.edge.decoboon.carryover.service.reconciliation.build.support;

import com.th4.edge.decoboon.carryover.context.CarryoverReconciliationContext;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationExport;
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

/** 双轨对账导出实现。 */
@Note("导出双轨对账表")
@Properties(allow = @AllowProperties(model = CarryoverReconciliationExport.class,
        properties = {"fileName", "contentType", "content"}))
@Service("carryover/reconciliation/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.carryover.service.reconciliation.build.Build {

    @User private Employee employee;
    @AutoInject @NotNull private Long reconciliationId;
    private final CarryoverReconciliationContext context;

    public Build(CarryoverReconciliationContext context) { this.context = context; }

    @Override
    @Transactional(readOnly = true)
    public CarryoverReconciliationExport call(CarryoverReconciliation request) {
        return context.export(employee, reconciliationId);
    }
}
