package com.th4.edge.decoboon.carryover.service.plan.stage.save.support;

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

import java.time.LocalDate;

@Note("保存接管后施工阶段")
@Properties(allow = {
        @AllowProperties(model = ProjectStage.class,
                properties = {"stageId", "project", "name", "stageCode", "sort", "weight", "status",
                        "progress", "baselineProgress", "plannedStart", "plannedEnd", "actualStart", "actualEnd", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "businessOrigin"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/plan/stage/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.carryover.service.plan.stage.save.Save {

    @User private Employee employee;
    @AutoInject @NotNull private Long itemId;
    @AutoInject private Long stageId;
    @AutoInject @NotNull @Length(max = 128) private String name;
    @AutoInject @Length(max = 64) private String stageCode;
    @AutoInject private Integer sort;
    @AutoInject @NotNull private LocalDate plannedStart;
    @AutoInject @NotNull private LocalDate plannedEnd;
    @AutoInject @Length(max = 500) private String reason;

    private final CarryoverPlanContext planContext;

    public Save(CarryoverPlanContext planContext) {
        this.planContext = planContext;
    }

    @Override
    @Transactional
    public ProjectStage call(ProjectStage request) {
        return planContext.saveStage(employee, itemId, stageId, name, stageCode, sort,
                plannedStart, plannedEnd, reason);
    }
}
