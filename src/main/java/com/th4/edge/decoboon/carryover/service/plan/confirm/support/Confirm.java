package com.th4.edge.decoboon.carryover.service.plan.confirm.support;

import com.th4.edge.decoboon.carryover.context.CarryoverPlanContext;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
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

@Note("确认接管后施工计划")
@Properties(allow = {
        @AllowProperties(model = CarryoverProject.class,
                properties = {"itemId", "status", "planStatus", "planVersion", "planConfirmedAt", "planConfirmedBy",
                        "project", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "no", "name", "businessOrigin"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/plan/confirm")
@Scope(ScopeTypes.PROTOTYPE)
public class Confirm implements com.th4.edge.decoboon.carryover.service.plan.confirm.Confirm {

    @User private Employee employee;
    @AutoInject @NotNull private Long itemId;
    @AutoInject @Length(max = 500) private String summary;

    private final CarryoverPlanContext planContext;

    public Confirm(CarryoverPlanContext planContext) {
        this.planContext = planContext;
    }

    @Override
    @Transactional
    public CarryoverProject call(CarryoverProject request) {
        return planContext.confirm(employee, itemId, summary);
    }
}
