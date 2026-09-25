package com.th4.edge.decoboon.carryover.service.plan.change.page.support;

import com.th4.edge.decoboon.carryover.context.CarryoverPlanContext;
import com.th4.edge.decoboon.carryover.model.CarryoverPlanChange;
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

import java.util.List;

@Note("接管后施工计划调整记录")
@Properties(allow = {
        @AllowProperties(model = CarryoverPlanChange.class,
                properties = {"changeId", "action", "stageName", "beforeSnapshot", "afterSnapshot", "reason",
                        "createdBy", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/plan/change/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.carryover.service.plan.change.page.Page {

    @User private Employee employee;
    @AutoInject @NotNull private Long itemId;

    private final CarryoverPlanContext planContext;

    public Page(CarryoverPlanContext planContext) {
        this.planContext = planContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarryoverPlanChange> call(CarryoverPlanChange request) {
        return planContext.changes(employee, itemId);
    }
}
