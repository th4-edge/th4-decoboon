package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverConfirmationRole;
import com.th4.edge.decoboon.carryover.configure.CarryoverPlanAction;
import com.th4.edge.decoboon.carryover.configure.CarryoverPlanStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverPlanChange;
import com.th4.edge.decoboon.carryover.model.CarryoverPlanConfirmation;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverPlanChangeRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverPlanConfirmationRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 接管后剩余施工计划维护。 */
@Note("接管后剩余施工计划")
@Component
public class CarryoverPlanContext {

    private static final String BASELINE_CODE = "CARRYOVER_BASELINE";

    private final CarryoverAccessContext accessContext;
    private final CarryoverProjectRepository itemRepository;
    private final CarryoverPlanConfirmationRepository confirmationRepository;
    private final CarryoverPlanChangeRepository changeRepository;
    private final ProjectAccessContext projectAccessContext;
    private final ProjectPermissionContext permissionContext;
    private final ProjectStageRepository stageRepository;
    private final ProjectTaskRepository taskRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final ProjectContext projectContext;

    public CarryoverPlanContext(CarryoverAccessContext accessContext,
                                CarryoverProjectRepository itemRepository,
                                CarryoverPlanConfirmationRepository confirmationRepository,
                                CarryoverPlanChangeRepository changeRepository,
                                ProjectAccessContext projectAccessContext,
                                ProjectPermissionContext permissionContext,
                                ProjectStageRepository stageRepository,
                                ProjectTaskRepository taskRepository,
                                AcceptanceRepository acceptanceRepository,
                                ProjectContext projectContext) {
        this.accessContext = accessContext;
        this.itemRepository = itemRepository;
        this.confirmationRepository = confirmationRepository;
        this.changeRepository = changeRepository;
        this.projectAccessContext = projectAccessContext;
        this.permissionContext = permissionContext;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.projectContext = projectContext;
    }

    @Transactional
    public ProjectStage saveStage(Employee principal, Long itemId, Long stageId, String name,
                                  String stageCode, Integer sort, LocalDate plannedStart,
                                  LocalDate plannedEnd, String reason) {
        Employee current = accessContext.requireEditor(principal);
        CarryoverProject item = requirePlanItem(current, itemId);
        Project project = requireProject(item);
        projectAccessContext.requirePlanEditor(project, current);

        if (name == null || name.isBlank()) throw invalid("阶段名称不能为空");
        if (plannedStart == null) throw invalid("计划开始日期不能为空");
        if (plannedEnd == null) throw invalid("计划完成日期不能为空");
        if (plannedEnd.isBefore(plannedStart)) throw invalid("计划完成日期不能早于开始日期");
        if (plannedStart.isBefore(item.getBatch().getAsOfDate())) {
            throw invalid("接管后阶段不能早于接管日期");
        }

        if (stageId == null) {
            requireReasonWhenNeeded(item, reason);
            return createStage(item, project, current, name, stageCode, sort, plannedStart, plannedEnd, reason);
        }

        ProjectStage stage = requireStage(project, stageId);
        if (isBaseline(stage)) throw invalid("接管基线阶段不能修改");
        requireNotExecuted(stage, "阶段已进入实际施工，不能直接修改计划");
        requireReasonWhenNeeded(item, reason);

        String before = snapshot(stage);
        stage.setName(name.trim());
        stage.setStageCode(normalizeStageCode(stageCode, stage.getSort()));
        if (sort != null) stage.setSort(Math.max(1, sort));
        stage.setPlannedStart(plannedStart);
        stage.setPlannedEnd(plannedEnd);
        ProjectStage saved = stageRepository.save(stage);
        recordChange(item, project, saved, CarryoverPlanAction.StageUpdate, before, snapshot(saved), reason, current);
        markDraft(item);
        projectContext.recalculatePlanFromSchedule(project);
        projectContext.refreshProject(project);
        return saved;
    }

    @Transactional
    public ProjectStage deleteStage(Employee principal, Long itemId, Long stageId, String reason) {
        Employee current = accessContext.requireEditor(principal);
        CarryoverProject item = requirePlanItem(current, itemId);
        Project project = requireProject(item);
        projectAccessContext.requirePlanEditor(project, current);
        ProjectStage stage = requireStage(project, stageId);
        if (isBaseline(stage)) throw invalid("接管基线阶段不能删除");
        requireNotExecuted(stage, "阶段已进入实际施工，不能删除");
        requireReasonWhenNeeded(item, reason);

        String before = snapshot(stage);
        CarryoverPlanChange change = buildChange(item, project, null, stage.getName(),
                CarryoverPlanAction.StageDelete, before, null, reason, current);
        changeRepository.save(change);
        changeRepository.detachStage(stage.getStageId());
        stageRepository.delete(stage);
        markDraft(item);
        normalizeSort(project);
        projectContext.recalculatePlanFromSchedule(project);
        projectContext.refreshProject(project);
        return stage;
    }

    @Transactional
    public List<ProjectStage> reorder(Employee principal, Long itemId, List<Long> stageIds, String reason) {
        Employee current = accessContext.requireEditor(principal);
        CarryoverProject item = requirePlanItem(current, itemId);
        Project project = requireProject(item);
        projectAccessContext.requirePlanEditor(project, current);

        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        List<ProjectStage> futureStages = stages.stream()
                .filter(stage -> !isBaseline(stage))
                .toList();
        if (stageIds == null || stageIds.size() != futureStages.size()) {
            throw invalid("调整顺序时必须提交全部后续阶段");
        }
        Set<Long> requested = new HashSet<>(stageIds);
        Set<Long> existing = futureStages.stream().map(ProjectStage::getStageId).collect(Collectors.toSet());
        if (!requested.equals(existing)) throw invalid("阶段顺序数据已变化，请刷新后重试");
        requireReasonWhenNeeded(item, reason);

        String before = stages.stream()
                .sorted(Comparator.comparing(ProjectStage::getSort))
                .map(ProjectStage::getName)
                .collect(Collectors.joining(" → "));
        java.util.Map<Long, ProjectStage> byId = futureStages.stream()
                .collect(Collectors.toMap(ProjectStage::getStageId, stage -> stage));
        for (int i = 0; i < stageIds.size(); i++) {
            ProjectStage stage = byId.get(stageIds.get(i));
            stage.setSort(i + 1);
            stageRepository.save(stage);
        }
        String after = stages.stream()
                .sorted(Comparator.comparing(ProjectStage::getSort))
                .map(ProjectStage::getName)
                .collect(Collectors.joining(" → "));
        recordChange(item, project, null, CarryoverPlanAction.StageReorder, before, after, reason, current);
        markDraft(item);
        projectContext.recalculatePlanFromSchedule(project);
        projectContext.refreshProject(project);
        return stageRepository.findByProjectOrderBySort(project);
    }

    @Transactional
    public CarryoverProject confirm(Employee principal, Long itemId, String summary) {
        Employee current = accessContext.requireEditor(principal);
        CarryoverProject item = requirePlanItem(current, itemId);
        Project project = requireProject(item);
        projectAccessContext.requirePlanEditor(project, current);
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.Pm
                && current.getRole() != EmployeeRole.StoreManager) {
            throw invalid("只有项目经理、企业负责人或本店店长可以确认接管后计划");
        }
        accessContext.requireSameStore(current, item);
        if (item.getPlanStatus() == CarryoverPlanStatus.Confirmed
                || item.getPlanStatus() == CarryoverPlanStatus.Locked) {
            return item;
        }
        if (stageRepository.findByProjectOrderBySort(project).isEmpty()) {
            throw invalid("至少需要一个施工阶段");
        }
        int version = item.getPlanVersion() == null || item.getPlanVersion() < 1
                ? 1 : item.getPlanVersion();
        item.setPlanVersion(version);
        item.setPlanConfirmedAt(LocalDateTime.now());
        item.setPlanConfirmedBy(current);
        item.setPlanStatus(item.getStatus() == CarryoverProjectStatus.Locked
                ? CarryoverPlanStatus.Locked : CarryoverPlanStatus.Confirmed);
        CarryoverProject saved = itemRepository.save(item);

        CarryoverPlanConfirmation confirmation = new CarryoverPlanConfirmation();
        confirmation.setCompany(saved.getCompany());
        confirmation.setItem(saved);
        confirmation.setProject(project);
        confirmation.setRequiredRole(CarryoverConfirmationRole.Pm);
        confirmation.setConfirmedBy(current);
        confirmation.setProxy(current.getRole() != EmployeeRole.Pm);
        confirmation.setPlanVersion(version);
        confirmation.setConfirmedAt(LocalDateTime.now());
        confirmation.setSummary(summary == null || summary.isBlank()
                ? "确认接管后剩余施工阶段和计划日期" : summary);
        confirmationRepository.save(confirmation);
        recordChange(saved, project, null, CarryoverPlanAction.Confirm, null,
                "版本 " + version + " 已确认", summary, current);
        return saved;
    }

    public List<CarryoverPlanChange> changes(Employee principal, Long itemId) {
        Employee current = accessContext.requireEditor(principal);
        CarryoverProject item = requirePlanItem(current, itemId);
        projectAccessContext.requireReadableProject(item.getProject().getProjectId(), current);
        return changeRepository.findByItem_ItemIdOrderByChangeIdDesc(itemId);
    }

    private ProjectStage createStage(CarryoverProject item, Project project, Employee current,
                                     String name, String stageCode, Integer sort,
                                     LocalDate plannedStart, LocalDate plannedEnd, String reason) {
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        int maxSort = 0;
        for (ProjectStage currentStage : stages) {
            if (currentStage.getSort() != null && currentStage.getSort() > maxSort) {
                maxSort = currentStage.getSort();
            }
        }
        int nextSort = sort == null || sort < 1 ? maxSort + 1 : sort;
        boolean sortOccupied = false;
        for (ProjectStage currentStage : stages) {
            if (Objects.equals(currentStage.getSort(), nextSort)) {
                sortOccupied = true;
                break;
            }
        }
        if (sortOccupied) nextSort = maxSort + 1;
        ProjectStage stage = new ProjectStage();
        stage.setCompany(project.getCompany());
        stage.setProject(project);
        stage.setProperty(project.getProperty());
        permissionContext.apply(stage, project);
        stage.setName(name.trim());
        stage.setStageCode(normalizeStageCode(stageCode, nextSort));
        stage.setSort(nextSort);
        stage.setWeight(BigDecimal.ONE);
        stage.setStatus(StageStatus.Todo);
        stage.setProgress(0);
        stage.setPlannedStart(plannedStart);
        stage.setPlannedEnd(plannedEnd);
        ProjectStage saved = stageRepository.save(stage);
        recordChange(item, project, saved, CarryoverPlanAction.StageCreate,
                null, snapshot(saved), reason, current);
        markDraft(item);
        projectContext.recalculatePlanFromSchedule(project);
        projectContext.refreshProject(project);
        return saved;
    }

    private void normalizeSort(Project project) {
        List<ProjectStage> stages = stageRepository.findByProjectOrderBySort(project);
        int sort = 1;
        for (ProjectStage stage : stages) {
            if (isBaseline(stage)) {
                stage.setSort(0);
            } else {
                stage.setSort(sort++);
            }
            stageRepository.save(stage);
        }
    }

    private void requireNotExecuted(ProjectStage stage, String message) {
        List<ProjectTask> tasks = taskRepository.findByStage(stage);
        boolean taskStarted = tasks.stream().anyMatch(task ->
                task.getActualStart() != null || (task.getProgress() != null && task.getProgress() > 0));
        if (stage.getActualStart() != null || (stage.getProgress() != null && stage.getProgress() > 0)
                || taskStarted || acceptanceRepository.findFirstByStageOrderByAcceptanceIdDesc(stage).isPresent()) {
            throw invalid(message);
        }
    }

    private void requireReasonWhenNeeded(CarryoverProject item, String reason) {
        if ((item.getPlanStatus() == CarryoverPlanStatus.Confirmed
                || item.getPlanStatus() == CarryoverPlanStatus.Locked
                || item.getStatus() == CarryoverProjectStatus.Locked)
                && (reason == null || reason.isBlank())) {
            throw invalid("调整已确认或已锁定的计划必须填写原因");
        }
    }

    private void markDraft(CarryoverProject item) {
        item.setPlanVersion((item.getPlanVersion() == null ? 0 : item.getPlanVersion()) + 1);
        boolean previouslyConfirmed = item.getPlanConfirmedAt() != null
                || item.getPlanStatus() == CarryoverPlanStatus.Confirmed
                || item.getPlanStatus() == CarryoverPlanStatus.Locked;
        if (item.getStatus() == CarryoverProjectStatus.Locked && previouslyConfirmed) {
            item.setPlanStatus(CarryoverPlanStatus.Locked);
        } else {
            item.setPlanConfirmedAt(null);
            item.setPlanConfirmedBy(null);
            item.setPlanStatus(CarryoverPlanStatus.Draft);
        }
        itemRepository.save(item);
    }

    private CarryoverProject requirePlanItem(Employee current, Long itemId) {
        CarryoverProject item = itemRepository
                .findByItemIdAndCompany_ComId(itemId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问")));
        if (item.getStatus() == CarryoverProjectStatus.Cancelled) {
            throw invalid("已取消的承接项目不能制定施工计划");
        }
        if (item.getProject() == null || item.getProject().getProjectId() == null) {
            throw invalid("请先完成项目承接，再制定剩余施工计划");
        }
        return item;
    }

    private Project requireProject(CarryoverProject item) {
        return item.getProject();
    }

    private ProjectStage requireStage(Project project, Long stageId) {
        ProjectStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "施工阶段不存在")));
        if (stage.getProject() == null || !Objects.equals(stage.getProject().getProjectId(), project.getProjectId())) {
            throw new DefinedException(new DefaultErrorCode("404", "施工阶段不存在或无权访问"));
        }
        return stage;
    }

    private boolean isBaseline(ProjectStage stage) {
        return BASELINE_CODE.equals(stage.getStageCode());
    }

    private String normalizeStageCode(String code, Integer sort) {
        if (code != null && !code.isBlank()) {
            return code.trim().toUpperCase().replaceAll("[^A-Z0-9_]+", "_");
        }
        return "CARRYOVER_REMAINING_" + (sort == null ? 1 : sort);
    }

    private String snapshot(ProjectStage stage) {
        if (stage == null) return null;
        return String.format("名称=%s；排序=%s；开始=%s；完成=%s；状态=%s；进度=%s",
                safe(stage.getName()), stage.getSort(), stage.getPlannedStart(), stage.getPlannedEnd(),
                stage.getStatus(), stage.getProgress());
    }

    private void recordChange(CarryoverProject item, Project project, ProjectStage stage,
                              CarryoverPlanAction action, String before, String after,
                              String reason, Employee current) {
        changeRepository.save(buildChange(item, project, stage, stage == null ? null : stage.getName(),
                action, before, after, reason, current));
    }

    private CarryoverPlanChange buildChange(CarryoverProject item, Project project, ProjectStage stage,
                                            String stageName, CarryoverPlanAction action,
                                            String before, String after, String reason,
                                            Employee current) {
        CarryoverPlanChange change = new CarryoverPlanChange();
        change.setCompany(item.getCompany());
        change.setItem(item);
        change.setProject(project);
        change.setStage(stage);
        change.setStageName(stageName);
        change.setAction(action);
        change.setBeforeSnapshot(trim(before, 2000));
        change.setAfterSnapshot(trim(after, 2000));
        change.setReason(trim(reason, 500));
        change.setCreatedBy(current);
        return change;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}
