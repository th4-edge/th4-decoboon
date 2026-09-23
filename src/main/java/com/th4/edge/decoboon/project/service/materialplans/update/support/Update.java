package com.th4.edge.decoboon.project.service.materialplans.update.support;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** 材料计划推进 Provider。 */
@Note("材料计划推进")
@Properties(allow = {
        @AllowProperties(model = MaterialPlan.class,
                properties = {"planId", "project", "task", "catalogItem", "quantity", "unit", "unitPrice",
                        "cost", "status", "requiredAt", "arrivedAt", "createTime", "property" }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr", "property" }),
        @AllowProperties(model = ProjectTask.class, properties = {"taskId", "name", "status", "progress"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "brand", "series", "spec"})
})
@Service("project/material-plan/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.project.service.materialplans.update.Update {

    @AutoInject
    @NotNull
    private Long planId;

    @AutoInject
    @NotNull
    private MaterialPlanStatus status;

    @AutoInject
    private LocalDate arrivedAt;

    @User
    private Employee employee;

    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectContext projectContext;

    public Update(MaterialPlanRepository materialPlanRepository,
                  ProjectAccessContext accessContext,
                  ProjectContext projectContext) {
        this.materialPlanRepository = materialPlanRepository;
        this.accessContext = accessContext;
        this.projectContext = projectContext;
    }

    @Override
    public MaterialPlan call(MaterialPlan request) {
        Employee current = accessContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.Pm
                && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色无权推进材料计划"));
        }
        MaterialPlan plan = materialPlanRepository.findOne(((Specification<MaterialPlan>) (root, query, builder) -> builder.equal(root.get("planId"), planId)).and(accessContext.visibleChild(employee, MaterialPlan.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料计划不存在")));
        accessContext.requireVisible(plan.getProject(), current);
        return projectContext.updateMaterialPlan(planId, current.getCompany().getComId(), status, arrivedAt);
    }
}
