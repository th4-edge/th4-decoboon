package com.th4.edge.decoboon.carryover.service.plan.stage.reorder.support;

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

import java.util.List;

@Note("调整接管后阶段顺序")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "name", "stageCode", "sort", "status", "plannedStart", "plannedEnd"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "businessOrigin"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/plan/stage/reorder")
@Scope(ScopeTypes.PROTOTYPE)
public class Reorder implements com.th4.edge.decoboon.carryover.service.plan.stage.reorder.Reorder {

    @User private Employee employee;
    @AutoInject @NotNull private Long itemId;
    @AutoInject @NotNull private List<Long> stageIds;
    @AutoInject @Length(max = 500) private String reason;

    private final CarryoverPlanContext planContext;

    public Reorder(CarryoverPlanContext planContext) {
        this.planContext = planContext;
    }

    @Override
    @Transactional
    public List<ProjectStage> call(ProjectStage request) {
        return planContext.reorder(employee, itemId, stageIds, reason);
    }
}
