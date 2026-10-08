package com.th4.edge.decoboon.project.service.plandeviations.page.support;

import com.th4.edge.decoboon.project.configure.PlanDeviationStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import com.th4.edge.decoboon.project.repository.ProjectPlanDeviationRepository;
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

/** 计划偏差分页实现。 */
@Note("计划偏差分页")
@Properties(allow = {
        @AllowProperties(model = ProjectPlanDeviation.class,
                properties = {"deviationId", "project", "contract", "baseEndDate", "expectedEndDate", "deviationDays",
                        "reasonType", "reason", "scheduleStrategy", "responsible", "dueDate", "status",
                        "confirmedBy", "confirmedAt", "newBaseEndDate", "receivableRecalculated",
                        "visibleToCustomer", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "siteAddr"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/plan-deviation/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.plandeviations.page.Page {

    @AutoInject
    private Long projectId;

    @AutoInject
    private PlanDeviationStatus status;

    @User
    private Employee employee;

    private final ProjectAccessContext accessContext;
    private final ProjectPlanDeviationRepository deviationRepository;

    public Page(ProjectAccessContext accessContext, ProjectPlanDeviationRepository deviationRepository) {
        this.accessContext = accessContext;
        this.deviationRepository = deviationRepository;
    }

    @Override
    public org.springframework.data.domain.Page<ProjectPlanDeviation> call(ProjectPlanDeviation request) {
        Employee current = accessContext.requireEmployee(employee);
        accessContext.requireProject(projectId, current);
        List<ProjectPlanDeviation> list = deviationRepository
                .findByProject_ProjectIdOrderByDeviationIdDesc(projectId).stream()
                .filter(item -> status == null || item.getStatus() == status)
                .sorted(Comparator.comparing(ProjectPlanDeviation::getDeviationId).reversed())
                .toList();
        return new PageImpl<>(list);
    }
}
