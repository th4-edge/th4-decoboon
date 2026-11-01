package com.th4.edge.decoboon.carryover.service.project.build.support;

import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverApplyContext;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
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

@Note("承接项目落地")
@Properties(allow = {
        @AllowProperties(model = CarryoverProject.class,
        properties = {"itemId", "status", "quote", "contract", "project", "receivable", "payable", "appliedAt", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.Payable.class, properties = {"payableId", "name", "type", "amount", "paidAmount", "dueDate", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.Receivable.class, properties = {"receivableId", "name", "type", "amount", "receivedAmount", "dueDate", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class, properties = {"projectId", "no", "name", "siteAddr", "status", "contractAmount"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.ProjectContract.class, properties = {"contractId", "no", "status", "signedAmount", "currentAmount", "startDate", "planEndDate"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.Quote.class, properties = {"quoteId", "no", "title", "status", "area", "amount"})
})
@Service("carryover/project/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.carryover.service.project.build.Build {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long itemId;

    private final CarryoverAccessContext accessContext;
    private final CarryoverApplyContext applyContext;

    public Build(CarryoverAccessContext accessContext, CarryoverApplyContext applyContext) {
        this.accessContext = accessContext;
        this.applyContext = applyContext;
    }

    @Override
    @Transactional
    public CarryoverProject call(CarryoverProject request) {
        Employee current = accessContext.requireManager(employee);
        CarryoverProject item = accessContext.requireItem(current, itemId);
        accessContext.requireSameStore(current, item);
        return applyContext.apply(item, current);
    }
}
