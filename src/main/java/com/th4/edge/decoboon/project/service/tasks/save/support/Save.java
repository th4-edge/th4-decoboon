package com.th4.edge.decoboon.project.service.tasks.save.support;

import com.th4.edge.decoboon.property.model.Property;
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
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
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
        if (taskId == null) {
            // DEC-049：施工任务只能由合同施工行或已生效的合同变更生成，排期页不得手工造任务。
            throw new DefinedException(new DefaultErrorCode(
                    "400", "施工任务只能来自本项目合同施工行或已生效的增减项，不能手工新增"));
        }
        Specification<ProjectTask> taskById = (root, query, builder) ->
                builder.equal(root.get("taskId"), taskId);
        ProjectTask task = taskRepository.findOne(taskById.and((root, query, builder) ->
                        builder.equal(root.get("project").get("projectId"), projectId)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "任务不存在或无权访问")));
        if (task.getStage() != null && com.th4.edge.decoboon.project.configure.StageStatus.NotApplicable.equals(task.getStage().getStatus())) {
            // DEC-057：任务落回被自动判定为「本项不涉及」的阶段时，自动恢复该阶段。
            ProjectStage source = task.getStage();
            source.setStatus(com.th4.edge.decoboon.project.configure.StageStatus.Todo);
            source.setNotApplicableReason(null);
            source.setNotApplicableBy(null);
            source.setNotApplicableAt(null);
            stageRepository.save(source);
        }
        if (task.getContractLine() == null && task.getChangeItem() == null) {
            // DEC-049：无来源的历史手工任务不能排期（本地环境直接清理，线上无此类数据）。
            throw new DefinedException(new DefaultErrorCode("400", "该施工任务缺少合同/变更来源，不能排期"));
        }
        Specification<ProjectStage> stageById = (root, query, builder) ->
                builder.equal(root.get("stageId"), stageId);
        ProjectStage stage = stageRepository.findOne(stageById.and((root, query, builder) ->
                        builder.equal(root.get("project").get("projectId"), projectId)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "阶段不存在或不属于当前项目")));
        // DEC-049：排期只允许调整阶段、区域、计划起止、责任人、优先级与备注；
        // 数量、名称、工艺、金额与成本由合同施工行/变更行决定，变化须走增减项。
        task.setProject(project);
        task.setProperty(project.getProperty());
        task.setStage(stage);
        task.setZone(resolveZone(zoneId));
        task.setPlannedStart(plannedStart);
        task.setPlannedEnd(plannedEnd);
        if (ownerId != null) {
            Employee owner = employeeRepository.findByEmpIdAndCompany_ComId(ownerId, current.getCompany().getComId());
            if (owner == null) {
                throw new DefinedException(new DefaultErrorCode("400", "任务责任人不存在或不属于当前企业"));
            }
            task.setOwner(owner);
        } else if (task.getOwner() == null) {
            task.setOwner(project.getManager() == null ? project.getOwner() : project.getManager());
        }
        if (priority != null && !priority.isBlank()) task.setPriority(priority);
        if (remark != null) task.setRemark(remark);
        if (task.getEvidenceUrls() == null) task.setEvidenceUrls(new ArrayList<>());
        ProjectTask saved = taskRepository.save(task);
        // DEC-048：排期只刷新项目预计完工日，不回写合同基准完工日与合同施工计划快照。
        projectContext.refreshExpectedEndFromSchedule(project);
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
