package com.th4.edge.decoboon.project.service.projects.picker.page.support;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 只服务项目选择器的轻量分页，避免复用项目台账的逐项目阶段/财务/状态 enrich。 */
@Note("项目选择器轻量分页")
@Properties(allow = {
        @AllowProperties(model = Project.class,
                properties = {"projectId", "businessOrigin", "no", "name", "property", "customer", "quote",
                        "designer", "manager", "support", "siteAddr", "area", "contractAmount", "status",
                        "progress", "startDate", "planEndDate", "currentStageName", "createTime"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "status"}),
        @AllowProperties(model = Quote.class, properties = {"quoteId", "amount", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/project/picker/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.projects.picker.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private Long customerId;
    @AutoInject private String keyword;
    @AutoInject private ProjectStatus status;
    @AutoInject private List<ProjectStatus> statuses;
    @AutoInject private Long designerId;
    @AutoInject private Long managerId;
    @AutoInject private BusinessOrigin businessOrigin;

    private final ProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectAccessContext accessContext;

    public Page(ProjectRepository projectRepository, ProjectStageRepository stageRepository,
                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Project> call(Project request) {
        Employee currentEmployee = accessContext.requireEmployee(employee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        Specification<Project> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (customerId != null) predicates.add(builder.equal(root.get("customer").get("custId"), customerId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (statuses != null && !statuses.isEmpty()) predicates.add(root.get("status").in(statuses));
            if (designerId != null) predicates.add(builder.equal(root.get("designer").get("empId"), designerId));
            if (managerId != null) predicates.add(builder.equal(root.get("manager").get("empId"), managerId));
            if (businessOrigin != null) predicates.add(builder.equal(root.get("businessOrigin"), businessOrigin));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("no")), pattern),
                        builder.like(builder.lower(root.get("siteAddr")), pattern),
                        builder.like(builder.lower(root.get("customer").get("name")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        org.springframework.data.domain.Page<Project> result = projectRepository.findAll(
                specification.and(accessContext.visibleReadableProject(currentEmployee)),
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "projectId"))
        );
        fillCurrentStages(result.getContent());
        return new PageImpl<>(result.getContent(), PageRequest.of(page, pageSize), result.getTotalElements());
    }

    private void fillCurrentStages(List<Project> projects) {
        if (projects.isEmpty()) return;
        List<Long> ids = projects.stream().map(Project::getProjectId).toList();
        Map<Long, String> names = new HashMap<>();
        for (ProjectStage stage : stageRepository.findByProject_ProjectIdInOrderByProjectProjectIdAscSortAsc(ids)) {
            Long projectId = stage.getProject().getProjectId();
            String current = names.get(projectId);
            if (current == null
                    || stage.getStatus() == StageStatus.Doing
                    || stage.getStatus() == StageStatus.Accepting) {
                names.put(projectId, stage.getName());
            }
        }
        projects.forEach(project -> project.setCurrentStageName(names.get(project.getProjectId())));
    }
}
