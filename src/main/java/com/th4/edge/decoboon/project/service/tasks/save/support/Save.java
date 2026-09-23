package com.th4.edge.decoboon.project.service.tasks.save.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

/** 由设计师从企业工艺库创建或调整施工任务。 */
@Note("施工任务保存")
@Properties(allow = {
        @AllowProperties(model = ProjectTask.class,
                properties = {"taskId", "project", "stage", "catalogItem", "owner", "creator", "name",
                        "zone", "craft", "spec", "quantity", "unit", "status", "progress", "priority",
                        "remark", "plannedStart", "plannedEnd", "actualStart", "actualEnd", "evidenceUrls", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name", "siteAddr", "property" }),
        @AllowProperties(model = ProjectStage.class, properties = {"stageId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = CatalogItem.class, properties = {"itemId", "code", "name", "unit", "craft", "spec"})
})
@Service("project/task/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.project.service.tasks.save.Save {

    @AutoInject
    private Long taskId;

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    @NotNull
    private Long stageId;

    @AutoInject
    private Long catalogId;

    @AutoInject
    private String zoneId;

    @AutoInject
    private BigDecimal quantity;

    @AutoInject
    private Long ownerId;

    @AutoInject
    private String name;

    @AutoInject
    private String craft;

    @AutoInject
    private String spec;

    @AutoInject
    private String priority;

    @AutoInject
    private String remark;

    @AutoInject
    private LocalDate plannedStart;

    @AutoInject
    private LocalDate plannedEnd;

    @User
    private Employee employee;

    private final ProjectRepository projectRepository;
    private final ProjectStageRepository stageRepository;
    private final ProjectTaskRepository taskRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectPermissionContext permissionContext;
    private final ProjectContext projectContext;

    public Save(ProjectRepository projectRepository,
                ProjectStageRepository stageRepository,
                ProjectTaskRepository taskRepository,
                CatalogItemRepository catalogItemRepository,
                EmployeeRepository employeeRepository,
                ProjectAccessContext accessContext,
                ProjectPermissionContext permissionContext,
                ProjectContext projectContext) {
        this.projectRepository = projectRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.employeeRepository = employeeRepository;
        this.accessContext = accessContext;
        this.permissionContext = permissionContext;
        this.projectContext = projectContext;
    }

    @Override
    @Transactional
    public ProjectTask call(ProjectTask request) {
        Employee current = accessContext.requireEmployee(employee);
        Project project = accessContext.requireProject(projectId, current);
        accessContext.requirePlanEditor(project, current);
        Specification<ProjectStage> stageById = (root, query, builder) ->
                builder.equal(root.get("stageId"), stageId);
        ProjectStage stage = stageRepository.findOne(stageById.and((root, query, builder) ->
                        builder.equal(root.get("project").get("projectId"), projectId)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "阶段不存在或不属于当前项目")));

        ProjectTask task;
        if (taskId == null) {
            task = new ProjectTask();
        } else {
            Specification<ProjectTask> taskById = (root, query, builder) ->
                    builder.equal(root.get("taskId"), taskId);
            task = taskRepository.findOne(taskById.and((root, query, builder) ->
                            builder.equal(root.get("project").get("projectId"), projectId)))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "任务不存在或无权访问")));
        }
        if (catalogId != null) {
            CatalogItem catalogItem = catalogItemRepository.findById(catalogId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工艺任务不存在")));
            if (catalogItem.getCompany() == null
                    || !catalogItem.getCompany().getComId().equals(current.getCompany().getComId())) {
                throw new DefinedException(new DefaultErrorCode("404", "工艺任务不存在或无权访问"));
            }
            task.setCatalogItem(catalogItem);
            task.setName(name == null || name.isBlank() ? catalogItem.getName() : name);
            task.setUnit(catalogItem.getUnit());
            task.setCraft(craft == null || craft.isBlank() ? catalogItem.getCraft() : craft);
            task.setSpec(spec == null ? catalogItem.getSpec() : spec);
            task.setConceal(isConceal(catalogItem));
        } else if (task.getCatalogItem() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "工艺任务不能为空"));
        }
        if (name != null && !name.isBlank()) task.setName(name);
        if (craft != null) task.setCraft(craft);
        if (spec != null) task.setSpec(spec);
        task.setCompany(current.getCompany());
        task.setProject(project);
        // ProjectTask.property 为非空列：新建任务必须显式带上项目房屋，
        // 否则真实排期接口在落库时抛 not-null 约束错误（DEF-PROJ37-005）。
        task.setProperty(project.getProperty());
        task.setStage(stage);
        permissionContext.apply(task, project);
        task.setZone(resolveZone(zoneId));
        task.setQuantity(quantity == null || quantity.signum() <= 0 ? BigDecimal.ONE : quantity);
        task.setPriority(priority == null || priority.isBlank() ? "normal" : priority);
        task.setRemark(remark);
        task.setPlannedStart(plannedStart);
        task.setPlannedEnd(plannedEnd);
        task.setCreator(task.getCreator() == null ? current : task.getCreator());
        Employee owner = ownerId == null ? project.getManager() : employeeRepository.findByEmpIdAndCompany_ComId(ownerId, current.getCompany().getComId());
        task.setOwner(owner);
        if (task.getEvidenceUrls() == null) task.setEvidenceUrls(new ArrayList<>());
        if (task.getTaskId() == null) {
            requireNotDispatched(project, stage, task);
        }
        ProjectTask saved = taskRepository.save(task);
        // 排期即计划：任务计划起止变化后按排期重算阶段/项目计划并回写合同施工计划快照。
        projectContext.recalculatePlanFromSchedule(project);
        return saved;
    }

    /**
     * 任务下发去重：同一「项目 × 阶段 × 区域 × 工艺任务」只允许一条未取消的任务。
     *
     * <p>历史缺陷：设计师排期可重复下发同一条工艺，产生多条重复任务（BUG-TASK-DUPLICATE-DISPATCH）。
     * 需要追加数量时应在原任务上编辑，而不是再次下发。</p>
     */
    private void requireNotDispatched(Project project, ProjectStage stage, ProjectTask task) {
        if (task.getCatalogItem() == null || task.getCatalogItem().getItemId() == null) {
            return;
        }
        boolean duplicated = taskRepository
                .findByProject_ProjectIdAndStage_StageIdAndCatalogItem_ItemId(
                        project.getProjectId(), stage.getStageId(), task.getCatalogItem().getItemId())
                .stream()
                .anyMatch(existing -> existing.getStatus() != TaskStatus.Cancelled
                        && java.util.Objects.equals(existing.getZone(), task.getZone()));
        if (duplicated) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该工艺任务已在「" + stage.getName() + " · " + (task.getZone() == null ? "未指定区域" : task.getZone())
                            + "」下发过，请在原任务上调整数量，不要重复下发"));
        }
    }

    private String resolveZone(String value) {
        if (value == null || value.isBlank()) return null;
        for (SpaceType type : SpaceType.values()) {
            if (type.getValue().equalsIgnoreCase(value) || type.getName().equals(value)) return type.getName();
        }
        return value;
    }

    private String join(java.util.List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("；", values);
    }

    private boolean isConceal(CatalogItem catalogItem) {
        String text = ((catalogItem.getName() == null ? "" : catalogItem.getName()) + " "
                + (catalogItem.getCode() == null ? "" : catalogItem.getCode())).toLowerCase();
        return text.contains("隐蔽") || text.contains("水电") || text.contains("防水")
                || text.contains("布线") || text.contains("管路") || text.contains("龙骨");
    }
}
