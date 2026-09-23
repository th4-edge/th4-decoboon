package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.ProjectRiskLevel;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.statistic.configure.WorkItemDomain;
import com.th4.edge.decoboon.statistic.configure.WorkbenchBucket;
import com.th4.edge.decoboon.statistic.model.SiteWorkItem;
import com.th4.edge.decoboon.statistic.model.SiteWorkbench;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 项目工地工作台聚合。
 *
 * <p>把计划、任务、材料、验收、整改与回款风险，从多条业务条线收敛成同一时点的「谁要做什么」。
 * 本上下文只做读取投影：不写业务事实、不改状态机、不新增金额口径。</p>
 *
 * <p>口径来源（保持一致，不另立规则）：项目逾期/临近完工、任务逾期、材料延误
 * 复用 {@code ProjectOverviewContext} 与 {@code PmReport} 的既有判定；
 * 成本不完整时不返回利润，只返回「成本未完整」提示。</p>
 */
@Component
public class SiteWorkbenchContext {

    /** 工地工作台只关心这些状态的项目。 */
    private static final List<ProjectStatus> SITE_STATUSES = List.of(
            ProjectStatus.Preparing, ProjectStatus.Ongoing,
            ProjectStatus.Suspended, ProjectStatus.Accepting);

    /** 单次聚合返回的工作项上限，避免非活跃项目把工作台淹没。 */
    private static final int ITEM_LIMIT = 240;

    /** 编排视图向前看的天数（含今天）。 */
    private static final int PLANNING_HORIZON_DAYS = 14;

    /**
     * 编排视图的「密集日」阈值。
     *
     * <p>只做提示，不自动改期、不评判产能。真实产能与工人配伍口径尚未裁决，
     * 见 {@code GAP-WB-005}；在此之前使用保守常量，不作为排期依据。</p>
     */
    private static final int PLANNING_DENSE_THRESHOLD = 4;

    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectStageRepository stageRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ReceivableRepository receivableRepository;
    private final ProjectAccessContext accessContext;

    public SiteWorkbenchContext(ProjectRepository projectRepository,
                                ProjectTaskRepository taskRepository,
                                ProjectStageRepository stageRepository,
                                AcceptanceRepository acceptanceRepository,
                                MaterialPlanRepository materialPlanRepository,
                                ReceivableRepository receivableRepository,
                                ProjectAccessContext accessContext) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.stageRepository = stageRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.receivableRepository = receivableRepository;
        this.accessContext = accessContext;
    }

    @Transactional(readOnly = true)
    public SiteWorkbench build(Employee principal) {
        LocalDate today = LocalDate.now();
        List<Project> projects = projectRepository.findAll(accessContext.visibleProject(principal)).stream()
                .filter(project -> SITE_STATUSES.contains(project.getStatus()))
                .sorted(Comparator.comparingInt(this::projectPriority)
                        .thenComparing(Project::getPlanEndDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Project::getProjectId))
                .toList();

        List<SiteWorkItem> items = new ArrayList<>();
        for (Project project : projects) {
            items.addAll(projectItems(project, today));
        }
        items.sort(itemComparator());

        SiteWorkbench workbench = new SiteWorkbench();
        workbench.setAsOf(today);
        workbench.setItems(items.size() > ITEM_LIMIT ? items.subList(0, ITEM_LIMIT) : items);
        workbench.setBucketCounts(bucketCounts(workbench.getItems()));
        workbench.setMetrics(metrics(projects, workbench.getItems()));
        workbench.setOwnerGroups(ownerGroups(workbench.getItems()));
        workbench.setPlanningStart(today);
        workbench.setPlanningEnd(today.plusDays(PLANNING_HORIZON_DAYS - 1L));
        workbench.setPlanningDays(planning(projects, today));
        return workbench;
    }

    private List<SiteWorkItem> projectItems(Project project, LocalDate today) {
        List<SiteWorkItem> items = new ArrayList<>();
        items.addAll(taskItems(project, today));
        items.addAll(acceptanceItems(project, today));
        items.addAll(materialItems(project, today));
        items.addAll(receivableItems(project, today));
        items.addAll(projectLevelItems(project, today));
        return items;
    }

    // ---------- 需要处理：任务 ----------

    private List<SiteWorkItem> taskItems(Project project, LocalDate today) {
        List<SiteWorkItem> items = new ArrayList<>();
        for (ProjectTask task : taskRepository.findByProject(project)) {
            if (!isTaskOpen(task)) {
                continue;
            }
            if (isTaskOverdue(task, today)) {
                long days = ChronoUnit.DAYS.between(task.getPlannedEnd(), today);
                SiteWorkItem item = base(project, "task-overdue-" + task.getTaskId(), WorkItemDomain.Project,
                        "TaskOverdue", "逾期任务", project);
                item.setSubjectType("ProjectTask");
                item.setSubjectId(task.getTaskId());
                item.setTitle(taskTitle(task, project));
                item.setOwnerName(employeeName(task.getOwner(), project.getManager()));
                item.setDerivedState("Overdue");
                item.setRiskLevel(ProjectRiskLevel.Attention);
                item.setStayDays(days);
                item.setDueAt(task.getPlannedEnd());
                item.setBucket(WorkbenchBucket.NeedAction);
                item.setReason("任务计划 " + task.getPlannedEnd() + " 完成，已逾期 " + days + " 天");
                item.setNextActionLabel("处理该任务");
                item.setNextActionType("OpenDrawer");
                item.setNextActionTarget("task:" + task.getTaskId());
                item.getMetrics().put("施工区域", nullToDash(task.getZone()));
                item.getMetrics().put("任务进度", (task.getProgress() == null ? 0 : task.getProgress()) + "%");
                items.add(item);
                continue;
            }
            if (isTaskToday(task, today)) {
                SiteWorkItem item = base(project, "task-today-" + task.getTaskId(), WorkItemDomain.Project,
                        "TaskToday", "今日施工", project);
                item.setSubjectType("ProjectTask");
                item.setSubjectId(task.getTaskId());
                item.setTitle(taskTitle(task, project));
                item.setOwnerName(employeeName(task.getOwner(), project.getManager()));
                item.setDerivedState(task.getStatus() == TaskStatus.Doing ? "Doing" : "Scheduled");
                item.setDueAt(today);
                item.setBucket(WorkbenchBucket.NeedAction);
                item.setReason(task.getStatus() == TaskStatus.Doing
                        ? "今日计划施工，当前进行中"
                        : "计划今日开工，尚未开始");
                item.setNextActionLabel(task.getStatus() == TaskStatus.Doing ? "记录施工进展" : "开始施工");
                item.setNextActionType("OpenDrawer");
                item.setNextActionTarget("task:" + task.getTaskId());
                item.getMetrics().put("施工区域", nullToDash(task.getZone()));
                item.getMetrics().put("计划结束", task.getPlannedEnd() == null ? "-" : task.getPlannedEnd().toString());
                items.add(item);
            }
        }
        return items;
    }

    // ---------- 需要处理 / 待确认：验收 ----------

    private List<SiteWorkItem> acceptanceItems(Project project, LocalDate today) {
        List<SiteWorkItem> items = new ArrayList<>();
        List<Acceptance> acceptances = acceptanceRepository
                .findByProject_ProjectIdOrderByAcceptanceIdDesc(project.getProjectId(), PageRequest.of(0, 50))
                .getContent();
        for (Acceptance acceptance : acceptances) {
            if (acceptance.getStatus() == AcceptStatus.Rejected) {
                long days = stayDays(acceptance.getCreateTime() == null ? null : acceptance.getCreateTime().toLocalDate(), today);
                SiteWorkItem item = base(project, "acceptance-rejected-" + acceptance.getAcceptanceId(),
                        WorkItemDomain.Acceptance, "AcceptanceRejected", "验收驳回", project);
                item.setSubjectType("Acceptance");
                item.setSubjectId(acceptance.getAcceptanceId());
                item.setTitle(stageTitle(acceptance) + " · 验收被驳回");
                item.setOwnerName(employeeName(acceptance.getRectificationOwner(), project.getManager()));
                item.setDerivedState("Rejected");
                item.setRiskLevel(ProjectRiskLevel.High);
                item.setStayDays(days);
                item.setBucket(WorkbenchBucket.NeedAction);
                item.setReason(firstIssue(acceptance) == null
                        ? "阶段验收被驳回，需整改后重新提交"
                        : "整改原因：" + firstIssue(acceptance));
                item.setNextActionLabel("提交整改");
                item.setNextActionType("OpenDrawer");
                item.setNextActionTarget("acceptance:" + acceptance.getAcceptanceId());
                if (acceptance.getRectificationDueDate() != null) {
                    item.setDueAt(acceptance.getRectificationDueDate());
                    item.getMetrics().put("整改截止", acceptance.getRectificationDueDate().toString());
                }
                items.add(item);
                continue;
            }
            if (acceptance.getStatus() == AcceptStatus.Pending) {
                long days = stayDays(acceptance.getCreateTime() == null ? null : acceptance.getCreateTime().toLocalDate(), today);
                SiteWorkItem item = base(project, "acceptance-pending-" + acceptance.getAcceptanceId(),
                        WorkItemDomain.Acceptance, "AcceptancePending", "待确认验收", project);
                item.setSubjectType("Acceptance");
                item.setSubjectId(acceptance.getAcceptanceId());
                item.setTitle(stageTitle(acceptance) + " · 待确认");
                item.setOwnerName(employeeName(acceptance.getSubmittedBy(), project.getManager()));
                item.setDerivedState("Submitted");
                item.setStayDays(days);
                item.setBucket(WorkbenchBucket.Risk);
                item.setReason("已提交阶段验收，等待客户确认" + (days > 0 ? "，已等待 " + days + " 天" : ""));
                item.setNextActionLabel("跟进确认");
                item.setNextActionType("OpenDrawer");
                item.setNextActionTarget("acceptance:" + acceptance.getAcceptanceId());
                items.add(item);
            }
        }
        return items;
    }

    // ---------- 风险：材料 ----------

    private List<SiteWorkItem> materialItems(Project project, LocalDate today) {
        List<SiteWorkItem> items = new ArrayList<>();
        for (MaterialPlan plan : materialPlanRepository.findByProject(project)) {
            if (!isMaterialDelayed(plan, today)) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(plan.getRequiredAt(), today);
            SiteWorkItem item = base(project, "material-delayed-" + plan.getPlanId(), WorkItemDomain.Material,
                    "MaterialDelayed", "材料延误", project);
            item.setSubjectType("MaterialPlan");
            item.setSubjectId(plan.getPlanId());
            item.setTitle(materialTitle(plan, project));
            item.setOwnerName(employeeName(plan.getOwner(), project.getSupport()));
            item.setDerivedState("Overdue");
            item.setRiskLevel(ProjectRiskLevel.Attention);
            item.setStayDays(days);
            item.setDueAt(plan.getRequiredAt());
            item.setBucket(WorkbenchBucket.Risk);
            item.setReason("要求 " + plan.getRequiredAt() + " 到场，已延误 " + days + " 天，未到场");
            item.setNextActionLabel("跟进到场");
            item.setNextActionType("OpenDrawer");
            item.setNextActionTarget("material:" + plan.getPlanId());
            item.getMetrics().put("计划数量", number(plan.getQuantity()) + unitName(plan));
            item.getMetrics().put("材料状态", plan.getStatus() == null ? "-" : plan.getStatus().getName());
            items.add(item);
        }
        return items;
    }

    // ---------- 风险：回款 ----------

    private List<SiteWorkItem> receivableItems(Project project, LocalDate today) {
        List<SiteWorkItem> items = new ArrayList<>();
        for (Receivable receivable : receivableRepository.findByProjectOrderByDueDateAsc(project)) {
            if (!isReceivableOverdue(receivable, today)) {
                continue;
            }
            long days = receivable.getDueDate() == null ? 0 : ChronoUnit.DAYS.between(receivable.getDueDate(), today);
            SiteWorkItem item = base(project, "receivable-overdue-" + receivable.getReceivableId(),
                    WorkItemDomain.Finance, "ReceivableOverdue", "回款逾期", project);
            item.setSubjectType("Receivable");
            item.setSubjectId(receivable.getReceivableId());
            item.setTitle((receivable.getName() == null ? "应收节点" : receivable.getName()) + " · 逾期未收");
            item.setOwnerName(project.getManager() == null ? null : project.getManager().getName());
            item.setDerivedState("Overdue");
            item.setRiskLevel(ProjectRiskLevel.High);
            item.setStayDays(days);
            item.setDueAt(receivable.getDueDate());
            item.setAmount(outstanding(receivable));
            item.setBucket(WorkbenchBucket.Risk);
            item.setReason("约定 " + receivable.getDueDate() + " 收款，已逾期 " + days
                    + " 天，未收 " + money(outstanding(receivable)) + " 元");
            item.setNextActionLabel("跟踪回款");
            // 契约：工作台内动作必须原地完成或进入另一个工作台，禁止跳对象台账页
            item.setNextActionType("OpenDrawer");
            item.setNextActionTarget("receivable:" + receivable.getReceivableId());
            items.add(item);
        }
        return items;
    }

    // ---------- 项目级：推进与风险 ----------

    private List<SiteWorkItem> projectLevelItems(Project project, LocalDate today) {
        List<SiteWorkItem> items = new ArrayList<>();
        String subtitle = projectSubtitle(project);
        String stageName = currentStageName(project);

        if (project.getStatus() == ProjectStatus.Suspended) {
            SiteWorkItem item = base(project, "project-suspended-" + project.getProjectId(), WorkItemDomain.Project,
                    "Suspended", "项目停工", project);
            item.setSubjectType("Project");
            item.setSubjectId(project.getProjectId());
            item.setDerivedState("Blocked");
            item.setRiskLevel(ProjectRiskLevel.High);
            item.setBucket(WorkbenchBucket.Risk);
            item.setReason("项目已停工，需要明确复工责任与日期");
            item.setNextActionLabel("核实复工");
            // 契约：不跳对象台账。项目级处置在当前工作台原地展开项目上下文。
            item.setNextActionType("OpenDrawer");
            item.setNextActionTarget("project:" + project.getProjectId());
            item.getMetrics().put("当前阶段", stageName);
            items.add(item);
            return items;
        }

        if (isProjectOverdue(project, today)) {
            long days = ChronoUnit.DAYS.between(project.getPlanEndDate(), today);
            SiteWorkItem item = base(project, "project-overdue-" + project.getProjectId(), WorkItemDomain.Project,
                    "Overdue", "工期逾期", project);
            item.setSubjectType("Project");
            item.setSubjectId(project.getProjectId());
            item.setDerivedState("Overdue");
            item.setRiskLevel(ProjectRiskLevel.High);
            item.setStayDays(days);
            item.setDueAt(project.getPlanEndDate());
            item.setBucket(WorkbenchBucket.Risk);
            item.setReason("计划完工 " + project.getPlanEndDate() + "，已逾期 " + days
                    + " 天，当前进度 " + progress(project) + "%");
            item.setNextActionLabel("核实交付计划");
            // 契约：不跳对象台账。项目级处置在当前工作台原地展开项目上下文。
            item.setNextActionType("OpenDrawer");
            item.setNextActionTarget("project:" + project.getProjectId());
            item.getMetrics().put("当前阶段", stageName);
            items.add(item);
            return items;
        }

        boolean ongoing = project.getStatus() == ProjectStatus.Ongoing
                || project.getStatus() == ProjectStatus.Accepting
                || project.getStatus() == ProjectStatus.Preparing;
        if (ongoing) {
            SiteWorkItem item = base(project, "project-progress-" + project.getProjectId(), WorkItemDomain.Project,
                    project.getStatus().name(), project.getStatus().getName(), project);
            item.setSubjectType("Project");
            item.setSubjectId(project.getProjectId());
            item.setDerivedState(project.getStatus() == ProjectStatus.Preparing ? "Pending" : "Doing");
            item.setBucket(WorkbenchBucket.InProgress);
            item.setReason(project.getStatus() == ProjectStatus.Preparing
                    ? "待开工：请确认开工准备与施工计划"
                    : "施工中，当前阶段「" + stageName + "」，进度 " + progress(project) + "%");
            item.setNextActionLabel(project.getStatus() == ProjectStatus.Preparing ? "确认开工准备" : "进入工地");
            // 契约：不跳对象台账。项目级处置在当前工作台原地展开项目上下文。
            item.setNextActionType("OpenDrawer");
            item.setNextActionTarget("project:" + project.getProjectId());
            item.setDueAt(project.getPlanEndDate());
            item.setSubtitle(subtitle);
            item.getMetrics().put("当前阶段", stageName);
            item.getMetrics().put("项目进度", progress(project) + "%");
            item.getMetrics().put("计划完工", project.getPlanEndDate() == null ? "-" : project.getPlanEndDate().toString());
            items.add(item);
        }
        return items;
    }

    // ---------- 编排视图（跨工地按天排布） ----------

    /**
     * 编排视图：把未来 {@value #PLANNING_HORIZON_DAYS} 天的关键节点按天排开，
     * 让负责人在同一屏看清「哪几天堆在一起」。
     *
     * <p>只投影已存在的计划事实（任务计划开工、材料要求到场、整改截止、应收到期），
     * 不推算日期、不新增计划口径、不自动改期。</p>
     */
    private List<SiteWorkbench.PlanningDay> planning(List<Project> projects, LocalDate today) {
        List<SiteWorkbench.PlanningItem> nodes = new ArrayList<>();
        for (Project project : projects) {
            nodes.addAll(planningNodes(project, today));
        }

        List<SiteWorkbench.PlanningDay> days = new ArrayList<>();
        for (int offset = 0; offset < PLANNING_HORIZON_DAYS; offset++) {
            LocalDate date = today.plusDays(offset);
            SiteWorkbench.PlanningDay day = new SiteWorkbench.PlanningDay();
            day.setDate(date);
            day.setWeekday(weekdayText(date));
            day.setToday(offset == 0);

            List<SiteWorkbench.PlanningItem> items = new ArrayList<>();
            for (SiteWorkbench.PlanningItem node : nodes) {
                LocalDate planned = nodeDueDate(node, today);
                // 今天列同时收纳已逾期未处理节点，避免它们从编排视图里消失
                boolean sameDay = offset == 0 ? !planned.isAfter(date) : planned.isEqual(date);
                if (sameDay) {
                    if (planned.isBefore(today)) {
                        node.setOverdue(Boolean.TRUE);
                    }
                    items.add(node);
                }
            }
            items.sort(Comparator.comparing(SiteWorkbench.PlanningItem::getKind)
                    .thenComparing(SiteWorkbench.PlanningItem::getProjectName,
                            Comparator.nullsLast(Comparator.naturalOrder())));
            day.setItems(items);
            day.setCount(items.size());
            boolean dense = items.size() >= PLANNING_DENSE_THRESHOLD;
            day.setDense(dense);
            day.setDenseHint(dense ? "当天关键节点 " + items.size() + " 项，建议提前确认工人与材料" : null);
            days.add(day);
        }
        return days;
    }

    private List<SiteWorkbench.PlanningItem> planningNodes(Project project, LocalDate today) {
        LocalDate limit = today.plusDays(PLANNING_HORIZON_DAYS - 1L);
        List<SiteWorkbench.PlanningItem> nodes = new ArrayList<>();

        for (ProjectTask task : taskRepository.findByProject(project)) {
            if (!isTaskOpen(task) || task.getPlannedStart() == null || task.getPlannedStart().isAfter(limit)) {
                continue;
            }
            SiteWorkbench.PlanningItem node = planningNode(project, "ProjectTask", task.getTaskId(),
                    "TaskStart", taskTitle(task, project), employeeName(task.getOwner(), project.getManager()));
            node.setPlannedDate(task.getPlannedStart());
            node.setReschedulable(Boolean.TRUE);
            node.setNextActionLabel("调整开工日");
            nodes.add(node);
        }

        for (MaterialPlan plan : materialPlanRepository.findByProject(project)) {
            if (plan.getRequiredAt() == null || plan.getRequiredAt().isAfter(limit)
                    || plan.getStatus() == MaterialPlanStatus.Arrived
                    || plan.getStatus() == MaterialPlanStatus.Closed) {
                continue;
            }
            SiteWorkbench.PlanningItem node = planningNode(project, "MaterialPlan", plan.getPlanId(),
                    "MaterialArrive", materialTitle(plan, project), employeeName(plan.getOwner(), project.getSupport()));
            node.setPlannedDate(plan.getRequiredAt());
            node.setReschedulable(Boolean.TRUE);
            node.setNextActionLabel("调整到场日");
            nodes.add(node);
        }

        List<Acceptance> acceptances = acceptanceRepository
                .findByProject_ProjectIdOrderByAcceptanceIdDesc(project.getProjectId(), PageRequest.of(0, 20))
                .getContent();
        for (Acceptance acceptance : acceptances) {
            if (acceptance.getStatus() != AcceptStatus.Rejected || acceptance.getRectificationDueDate() == null
                    || acceptance.getRectificationDueDate().isAfter(limit)) {
                continue;
            }
            SiteWorkbench.PlanningItem node = planningNode(project, "Acceptance", acceptance.getAcceptanceId(),
                    "RectifyDue", stageTitle(acceptance) + " · 整改截止",
                    employeeName(acceptance.getRectificationOwner(), project.getManager()));
            node.setPlannedDate(acceptance.getRectificationDueDate());
            node.setNextActionLabel("提交整改");
            nodes.add(node);
        }

        for (Receivable receivable : receivableRepository.findByProjectOrderByDueDateAsc(project)) {
            if (receivable.getDueDate() == null || receivable.getDueDate().isAfter(limit)
                    || receivable.getStatus() == ReceivableStatus.Received
                    || receivable.getStatus() == ReceivableStatus.Closed) {
                continue;
            }
            SiteWorkbench.PlanningItem node = planningNode(project, "Receivable", receivable.getReceivableId(),
                    "ReceivableDue", (receivable.getName() == null ? "应收节点" : receivable.getName()) + " · 到账",
                    employeeName(project.getManager(), project.getSupport()));
            node.setPlannedDate(receivable.getDueDate());
            node.setNextActionLabel("跟踪回款");
            nodes.add(node);
        }
        return nodes;
    }

    private SiteWorkbench.PlanningItem planningNode(Project project, String subjectType, Long subjectId,
                                                    String kind, String title, String ownerName) {
        SiteWorkbench.PlanningItem node = new SiteWorkbench.PlanningItem();
        node.setSubjectType(subjectType);
        node.setSubjectId(subjectId);
        node.setProjectId(project.getProjectId());
        node.setProjectName(project.getName());
        node.setKind(kind);
        node.setTitle(title);
        node.setOwnerName(ownerName);
        return node;
    }

    /** 编排节点所在的计划日：建节点时已从既有字段取好，不再重查仓储。 */
    private LocalDate nodeDueDate(SiteWorkbench.PlanningItem node, LocalDate today) {
        return node.getPlannedDate() == null ? today : node.getPlannedDate();
    }

    private String weekdayText(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    // ---------- 汇总 ----------

    private SiteWorkItem base(Project project, String itemKey, WorkItemDomain domain,
                              String stage, String stageName, Project projectRef) {
        SiteWorkItem item = new SiteWorkItem();
        item.setItemKey(itemKey);
        item.setDomain(domain);
        item.setStage(stage);
        item.setStageName(stageName);
        item.setProjectId(projectRef.getProjectId());
        item.setTitle(project.getName());
        item.setSubtitle(projectSubtitle(projectRef));
        item.setOwnerName(employeeName(projectRef.getManager(), projectRef.getSupport()));
        item.setRiskLevel(ProjectRiskLevel.Normal);
        return item;
    }

    private List<SiteWorkbench.BucketCount> bucketCounts(List<SiteWorkItem> items) {
        return List.of(
                new SiteWorkbench.BucketCount(WorkbenchBucket.NeedAction, WorkbenchBucket.NeedAction.getName(),
                        count(items, WorkbenchBucket.NeedAction)),
                new SiteWorkbench.BucketCount(WorkbenchBucket.InProgress, WorkbenchBucket.InProgress.getName(),
                        count(items, WorkbenchBucket.InProgress)),
                new SiteWorkbench.BucketCount(WorkbenchBucket.Risk, WorkbenchBucket.Risk.getName(),
                        count(items, WorkbenchBucket.Risk)));
    }

    private List<SiteWorkbench.Metric> metrics(List<Project> projects, List<SiteWorkItem> items) {
        int needAction = count(items, WorkbenchBucket.NeedAction);
        int risk = count(items, WorkbenchBucket.Risk);
        long overdue = items.stream()
                .filter(item -> "Overdue".equals(item.getDerivedState()))
                .count();
        return List.of(
                new SiteWorkbench.Metric("projects", "在管项目", projects.size(), "待开工/施工中/停工/待验收", "brand"),
                new SiteWorkbench.Metric("needAction", "需要处理", needAction, "任务、整改与今日施工", "warning"),
                new SiteWorkbench.Metric("overdue", "已逾期", (int) overdue, "任务、材料、回款与工期", "danger"),
                new SiteWorkbench.Metric("risk", "风险与待确认", risk, "停工、材料、回款与待确认验收", "purple"));
    }

    private List<SiteWorkbench.OwnerGroup> ownerGroups(List<SiteWorkItem> items) {
        Map<String, SiteWorkbench.OwnerGroup> groups = new LinkedHashMap<>();
        for (SiteWorkItem item : items) {
            String key = item.getOwnerName();
            SiteWorkbench.OwnerGroup group = groups.computeIfAbsent(key, ignored -> {
                SiteWorkbench.OwnerGroup created = new SiteWorkbench.OwnerGroup();
                created.setOwnerName(key);
                return created;
            });
            group.setCount(group.getCount() + 1);
            if (item.getBucket() == WorkbenchBucket.NeedAction) {
                group.setNeedActionCount(group.getNeedActionCount() + 1);
            }
            if (item.getBucket() == WorkbenchBucket.Risk) {
                group.setRiskCount(group.getRiskCount() + 1);
            }
        }
        return groups.values().stream()
                .sorted(Comparator.comparingInt(SiteWorkbench.OwnerGroup::getCount).reversed()
                        .thenComparing(SiteWorkbench.OwnerGroup::getOwnerName))
                .toList();
    }

    private Comparator<SiteWorkItem> itemComparator() {
        return Comparator.comparingInt((SiteWorkItem item) -> switch (item.getBucket()) {
                    case Risk -> 0;
                    case NeedAction -> 1;
                    case InProgress -> 2;
                })
                .thenComparingInt(item -> "Overdue".equals(item.getDerivedState()) ? 0 : 1)
                .thenComparing(SiteWorkItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SiteWorkItem::getStayDays, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int count(List<SiteWorkItem> items, WorkbenchBucket bucket) {
        return (int) items.stream().filter(item -> item.getBucket() == bucket).count();
    }

    // ---------- 既有口径复用 ----------

    /** 与 PmReport#isTaskOpen 同一口径。 */
    private boolean isTaskOpen(ProjectTask task) {
        return task.getStatus() != TaskStatus.Done && task.getStatus() != TaskStatus.Cancelled;
    }

    /** 与 PmReport#isTaskOverdue 同一口径。 */
    private boolean isTaskOverdue(ProjectTask task, LocalDate today) {
        return task.getPlannedEnd() != null && task.getPlannedEnd().isBefore(today) && isTaskOpen(task);
    }

    /** 与 PmReport#isProjectOverdue 同一口径。 */
    private boolean isProjectOverdue(Project project, LocalDate today) {
        return project.getPlanEndDate() != null
                && project.getPlanEndDate().isBefore(today)
                && project.getStatus() != ProjectStatus.Finished
                && project.getStatus() != ProjectStatus.Settled;
    }

    /** 与 PmReport#isMaterialDelayed 同一口径。 */
    private boolean isMaterialDelayed(MaterialPlan plan, LocalDate today) {
        return plan.getRequiredAt() != null
                && plan.getRequiredAt().isBefore(today)
                && plan.getStatus() != MaterialPlanStatus.Arrived
                && plan.getStatus() != MaterialPlanStatus.Closed;
    }

    /** 与 ProjectOverviewContext 的逾期应收口径一致。 */
    private boolean isReceivableOverdue(Receivable receivable, LocalDate today) {
        if (receivable.getStatus() == ReceivableStatus.Received
                || receivable.getStatus() == ReceivableStatus.Closed) {
            return false;
        }
        return receivable.getStatus() == ReceivableStatus.Overdue
                || (receivable.getDueDate() != null && receivable.getDueDate().isBefore(today));
    }

    private boolean isTaskToday(ProjectTask task, LocalDate today) {
        return task.getPlannedStart() != null && !task.getPlannedStart().isAfter(today)
                && isTaskOpen(task) && !isTaskOverdue(task, today);
    }

    // ---------- 展示辅助 ----------

    private int projectPriority(Project project) {
        if (project.getStatus() == ProjectStatus.Suspended) return 0;
        if (isProjectOverdue(project, LocalDate.now())) return 1;
        if (project.getStatus() == ProjectStatus.Accepting) return 2;
        if (project.getStatus() == ProjectStatus.Ongoing) return 3;
        return 4;
    }

    private String currentStageName(Project project) {
        return stageRepository.findByProjectOrderBySort(project).stream()
                .filter(stage -> stage.getStatus() == StageStatus.Doing)
                .map(ProjectStage::getName)
                .findFirst()
                .orElseGet(() -> project.getStatus() == null ? "-" : project.getStatus().getName());
    }

    private String projectSubtitle(Project project) {
        Property property = project.getProperty();
        Customer customer = project.getCustomer();
        String house = property == null ? null
                : joinNonBlank(property.getCommunity(), property.getAddress());
        String customerName = customer == null ? null : customer.getName();
        return joinNonBlank(project.getNo(), customerName, house);
    }

    private String taskTitle(ProjectTask task, Project project) {
        String zone = task.getZone() == null || task.getZone().isBlank() ? "" : task.getZone() + " · ";
        return project.getName() + " · " + zone + (task.getName() == null ? "施工任务" : task.getName());
    }

    private String stageTitle(Acceptance acceptance) {
        return acceptance.getStage() == null || acceptance.getStage().getName() == null
                ? "阶段验收" : acceptance.getStage().getName() + "验收";
    }

    private String materialTitle(MaterialPlan plan, Project project) {
        CatalogItem item = plan.getCatalogItem();
        String name = item != null && item.getName() != null ? item.getName() : "材料计划";
        return project.getName() + " · " + name;
    }

    private String firstIssue(Acceptance acceptance) {
        if (acceptance.getIssues() == null || acceptance.getIssues().isEmpty()) {
            return null;
        }
        return acceptance.getIssues().get(0);
    }

    /**
     * 责任人展示名。
     *
     * <p>契约要求责任人口径由服务端统一：没有责任人时一律返回「未分配」，
     * 避免各页面各自兜底导致分组与筛选对不上。</p>
     */
    private String employeeName(Employee preferred, Employee fallback) {
        if (preferred != null && preferred.getName() != null && !preferred.getName().isBlank()) {
            return preferred.getName();
        }
        if (fallback != null && fallback.getName() != null && !fallback.getName().isBlank()) {
            return fallback.getName();
        }
        return "未分配";
    }

    private long stayDays(LocalDate from, LocalDate today) {
        return from == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(from, today));
    }

    private BigDecimal outstanding(Receivable receivable) {
        BigDecimal amount = receivable.getAmount() == null ? BigDecimal.ZERO : receivable.getAmount();
        BigDecimal received = receivable.getReceivedAmount() == null ? BigDecimal.ZERO : receivable.getReceivedAmount();
        return amount.subtract(received).max(BigDecimal.ZERO);
    }

    private String money(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String number(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String unitName(MaterialPlan plan) {
        return plan.getUnit() == null ? "" : plan.getUnit().getName();
    }

    private String progress(Project project) {
        return String.valueOf(project.getProgress() == null ? 0 : project.getProgress());
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String joinNonBlank(String... parts) {
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                values.add(part);
            }
        }
        return String.join(" · ", values);
    }
}
