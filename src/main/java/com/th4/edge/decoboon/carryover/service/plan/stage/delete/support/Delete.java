package com.th4.edge.decoboon.carryover.service.plan.stage.delete.support;

import com.th4.edge.decoboon.carryover.context.CarryoverPlanContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
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

@Note("删除接管后施工阶段")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "name", "stageCode", "sort", "status", "plannedStart", "plannedEnd"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "businessOrigin"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/plan/stage/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.carryover.service.plan.stage.delete.Delete {

    @User private Employee employee;
    @AutoInject @NotNull private Long itemId;
    @AutoInject @NotNull private Long stageId;
    @AutoInject @Length(max = 500) private String reason;

    private final CarryoverPlanContext planContext;

    public Delete(CarryoverPlanContext planContext) {
        this.planContext = planContext;
    }

    @Override
    @Transactional
    public ProjectStage call(ProjectStage request) {
        return planContext.deleteStage(employee, itemId, stageId, reason);
    }
}
