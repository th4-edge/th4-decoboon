package com.th4.edge.decoboon.project.service.materialplans.page.support;
import com.th4.edge.decoboon.project.model.ProjectTask;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 材料计划分页实现。 */
@Note("材料计划分页")
@Properties(allow = {
        @AllowProperties(model = MaterialPlan.class,
                properties = {"planId", "project", "task", "catalogItem", "materialKey", "selectedBrand", "selectedSpec", "selectedModel", "netQuantity", "lossRate", "lossQuantity", "quantity", "unit", "unitPrice", "cost", "status",
                        "requiredAt", "arrivedAt", "createTime" }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr" }),
        @AllowProperties(model = ProjectTask.class, properties = {"taskId", "name", "status", "progress"}),
        @AllowProperties(model = CatalogItem.class, properties = {"itemId", "type", "name", "unit", "brand", "series", "spec"})
})
@Service("project/material-plan/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.materialplans.page.Page {

    @AutoInject
    private Long projectId;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectAccessContext accessContext;

    public Page(ProjectRepository projectRepository,
                MaterialPlanRepository materialPlanRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<MaterialPlan> call(MaterialPlan request) {
        Employee current = accessContext.requireEmployee(employee);
        List<Project> projects;
        if (projectId != null) {
            projects = List.of(accessContext.requireProject(projectId, current));
        } else {
            projects = visibleProjects(current);
        }
        List<MaterialPlan> plans = projects.stream()
                .flatMap(project -> materialPlanRepository.findByProject(project).stream())
                .sorted(Comparator.comparing(MaterialPlan::getPlanId))
                .toList();
        return new PageImpl<>(plans);
    }

    private List<Project> visibleProjects(Employee current) {
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        return switch (current.getRole()) {
            case Boss, Finance -> projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId);
            case StoreManager -> current.getStore() == null || current.getStore().getStoreId() == null
                    ? List.of()
                    : projectRepository.findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(
                            comId, current.getStore().getStoreId());
            case TeamLeader -> current.getTeam() == null || current.getTeam().getTeamId() == null
                    ? List.of()
                    : projectRepository.findByCompany_ComIdAndCustomer_Team_TeamIdOrderByProjectIdDesc(
                            comId, current.getTeam().getTeamId());
            case Designer -> projectRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByProjectIdDesc(comId, empId);
            case Pm -> projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(comId, empId);
            case Marketer -> projectRepository.findByCompany_ComIdAndQuote_CreatedBy_EmpIdOrderByProjectIdDesc(comId, empId);
            default -> List.of();
        };
    }
}
