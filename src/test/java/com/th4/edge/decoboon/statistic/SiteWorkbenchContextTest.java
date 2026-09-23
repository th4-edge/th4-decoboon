package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.ProjectRiskLevel;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.statistic.configure.WorkbenchBucket;
import com.th4.edge.decoboon.statistic.context.SiteWorkbenchContext;
import com.th4.edge.decoboon.statistic.model.SiteWorkItem;
import com.th4.edge.decoboon.statistic.model.SiteWorkbench;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 项目工地工作台读模型回归。
 *
 * <p>覆盖：三栏归属、派生状态、原因必填、责任人分组与栏位计数一致。</p>
 */
class SiteWorkbenchContextTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void shouldBucketOverdueTaskMaterialAndProjectProgress() {
        Fixture fixture = fixture();

        SiteWorkbench workbench = fixture.context.build(fixture.pm);

        assertThat(workbench.getAsOf()).isEqualTo(today);
        assertThat(workbench.getItems()).isNotEmpty();

        // 逾期任务进入「需要处理」
        SiteWorkItem overdueTask = item(workbench, "task-overdue-200");
        assertThat(overdueTask.getBucket()).isEqualTo(WorkbenchBucket.NeedAction);
        assertThat(overdueTask.getDerivedState()).isEqualTo("Overdue");
        assertThat(overdueTask.getRiskLevel()).isEqualTo(ProjectRiskLevel.Attention);
        assertThat(overdueTask.getStayDays()).isPositive();
        assertThat(overdueTask.getNextActionLabel()).isEqualTo("处理该任务");

        // 材料延误进入「风险与待确认」
        SiteWorkItem delayedMaterial = item(workbench, "material-delayed-300");
        assertThat(delayedMaterial.getBucket()).isEqualTo(WorkbenchBucket.Risk);
        assertThat(delayedMaterial.getDerivedState()).isEqualTo("Overdue");
        assertThat(delayedMaterial.getReason()).contains("延误");

        // 逾期应收进入风险栏，金额口径为未收金额
        SiteWorkItem overdueReceivable = item(workbench, "receivable-overdue-400");
        assertThat(overdueReceivable.getBucket()).isEqualTo(WorkbenchBucket.Risk);
        assertThat(overdueReceivable.getAmount()).isEqualByComparingTo("7000");

        // 在管项目本身进入「推进中」
        SiteWorkItem progress = item(workbench, "project-progress-100");
        assertThat(progress.getBucket()).isEqualTo(WorkbenchBucket.InProgress);
        assertThat(progress.getDerivedState()).isEqualTo("Doing");
        assertThat(progress.getReason()).contains("施工中");
    }

    @Test
    void shouldKeepReasonAndSingleActionOnEveryItem() {
        Fixture fixture = fixture();

        SiteWorkbench workbench = fixture.context.build(fixture.pm);

        assertThat(workbench.getItems()).allSatisfy(item -> {
            assertThat(item.getItemKey()).isNotBlank();
            // 契约：没有原因的卡片不允许出现在工作台
            assertThat(item.getReason()).isNotBlank();
            assertThat(item.getNextActionLabel()).isNotBlank();
            assertThat(item.getBucket()).isNotNull();
            assertThat(item.getOwnerName()).isNotNull();
        });
    }

    @Test
    void shouldKeepBucketCountsConsistentWithItems() {
        Fixture fixture = fixture();

        SiteWorkbench workbench = fixture.context.build(fixture.pm);

        for (SiteWorkbench.BucketCount count : workbench.getBucketCounts()) {
            long actual = workbench.getItems().stream()
                    .filter(item -> item.getBucket() == count.getBucket())
                    .count();
            assertThat(count.getCount()).isEqualTo((int) actual);
        }
        assertThat(workbench.getOwnerGroups()).anySatisfy(group -> {
            assertThat(group.getOwnerName()).isEqualTo("项目经理甲");
            assertThat(group.getCount()).isPositive();
            assertThat(group.getRiskCount()).isPositive();
        });
    }

    @Test
    void shouldLayPlanningNodesOnTheirExistingPlannedDate() {
        Fixture fixture = fixture();

        SiteWorkbench workbench = fixture.context.build(fixture.pm);

        // 编排窗口固定向前看 14 天
        assertThat(workbench.getPlanningDays()).hasSize(14);
        assertThat(workbench.getPlanningStart()).isEqualTo(today);
        assertThat(workbench.getPlanningEnd()).isEqualTo(today.plusDays(13));
        assertThat(workbench.getPlanningDays().get(0).getToday()).isTrue();

        // 逾期任务收敛到「今天」列并标记逾期，不从编排视图消失
        SiteWorkbench.PlanningDay todayColumn = workbench.getPlanningDays().get(0);
        assertThat(todayColumn.getItems())
                .anySatisfy(node -> assertThat(node.getKind()).isEqualTo("TaskStart"));
        assertThat(todayColumn.getItems()).anySatisfy(node -> {
            if ("TaskStart".equals(node.getKind())) {
                assertThat(node.getOverdue()).isTrue();
                assertThat(node.getProjectId()).isEqualTo(100L);
                assertThat(node.getReschedulable()).isTrue();
            }
        });
    }

    @Test
    void shouldMarkDenseDayWithoutJudgingCapacity() {
        Fixture fixture = fixture();

        SiteWorkbench workbench = fixture.context.build(fixture.pm);

        // 只有达到阈值才提示，且只给提示文案，不给产能结论
        assertThat(workbench.getPlanningDays()).allSatisfy(day -> {
            if (Boolean.TRUE.equals(day.getDense())) {
                assertThat(day.getDenseHint()).isNotBlank();
                assertThat(day.getCount()).isGreaterThanOrEqualTo(4);
            } else {
                assertThat(day.getDenseHint()).isNull();
            }
        });
    }

    @Test
    void shouldNeverEscapeWorkbenchToLedgerPage() {
        Fixture fixture = fixture();

        SiteWorkbench workbench = fixture.context.build(fixture.pm);

        // 契约：工作台是业务闭环页面，动作必须原地完成或进入另一个工作台；
        // 禁止跳对象台账页，且原地操作必须能定位到所属项目。
        assertThat(workbench.getItems()).allSatisfy(item -> {
            assertThat(item.getNextActionType())
                    .as("工作台动作不得为 OpenList/OpenWorkbench 之外的越界跳转")
                    .isIn("OpenDrawer", "None");
            assertThat(item.getProjectId()).isNotNull();
        });
    }

    @Test
    void shouldExcludeClosedTaskAndSortRiskFirst() {
        Fixture fixture = fixture();

        SiteWorkbench workbench = fixture.context.build(fixture.pm);

        assertThat(workbench.getItems())
                .noneMatch(item -> "task-overdue-201".equals(item.getItemKey()));
        // 风险优先于需要处理、再优先于推进中
        assertThat(workbench.getItems().get(0).getBucket()).isEqualTo(WorkbenchBucket.Risk);
    }

    // ---------- fixture ----------

    private record Fixture(SiteWorkbenchContext context, Employee pm) { }

    private Fixture fixture() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectTaskRepository tasks = mock(ProjectTaskRepository.class);
        ProjectStageRepository stages = mock(ProjectStageRepository.class);
        AcceptanceRepository acceptances = mock(AcceptanceRepository.class);
        MaterialPlanRepository materials = mock(MaterialPlanRepository.class);
        ReceivableRepository receivables = mock(ReceivableRepository.class);
        ProjectAccessContext access = mock(ProjectAccessContext.class);

        Company company = new Company();
        company.setComId(10L);
        Employee pm = new Employee();
        pm.setEmpId(11L);
        pm.setName("项目经理甲");
        pm.setCompany(company);

        Project ongoing = new Project();
        ongoing.setProjectId(100L);
        ongoing.setNo("P-100");
        ongoing.setName("金域华府 3-1802");
        ongoing.setCompany(company);
        ongoing.setManager(pm);
        ongoing.setStatus(ProjectStatus.Ongoing);
        ongoing.setProgress(45);
        ongoing.setPlanEndDate(today.plusDays(20));

        ProjectStage doing = new ProjectStage();
        doing.setStageId(150L);
        doing.setName("水电改造");
        doing.setStatus(StageStatus.Doing);

        ProjectTask overdue = new ProjectTask();
        overdue.setTaskId(200L);
        overdue.setProject(ongoing);
        overdue.setName("强弱电布管");
        overdue.setZone("全屋");
        overdue.setStatus(TaskStatus.Doing);
        overdue.setProgress(30);
        overdue.setPlannedStart(today.minusDays(6));
        overdue.setPlannedEnd(today.minusDays(2));
        overdue.setOwner(pm);

        ProjectTask closed = new ProjectTask();
        closed.setTaskId(201L);
        closed.setProject(ongoing);
        closed.setName("已取消拆改");
        closed.setStatus(TaskStatus.Cancelled);
        closed.setPlannedEnd(today.minusDays(9));

        MaterialPlan delayed = new MaterialPlan();
        delayed.setPlanId(300L);
        delayed.setProject(ongoing);
        delayed.setStatus(MaterialPlanStatus.Ordered);
        delayed.setRequiredAt(today.minusDays(3));
        delayed.setQuantity(new BigDecimal("12"));

        Receivable receivable = new Receivable();
        receivable.setReceivableId(400L);
        receivable.setProject(ongoing);
        receivable.setName("水电阶段款");
        receivable.setAmount(new BigDecimal("10000"));
        receivable.setReceivedAmount(new BigDecimal("3000"));
        receivable.setDueDate(today.minusDays(5));
        receivable.setStatus(ReceivableStatus.Partial);

        when(access.visibleProject(pm)).thenReturn(Specification.where(null));
        when(projects.findAll(any(Specification.class))).thenReturn(List.of(ongoing));
        when(tasks.findByProject(ongoing)).thenReturn(List.of(overdue, closed));
        when(stages.findByProjectOrderBySort(ongoing)).thenReturn(List.of(doing));
        when(acceptances.findByProject_ProjectIdOrderByAcceptanceIdDesc(anyLong(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(materials.findByProject(ongoing)).thenReturn(List.of(delayed));
        when(receivables.findByProjectOrderByDueDateAsc(ongoing)).thenReturn(List.of(receivable));

        SiteWorkbenchContext context = new SiteWorkbenchContext(
                projects, tasks, stages, acceptances, materials, receivables, access);
        return new Fixture(context, pm);
    }

    private SiteWorkItem item(SiteWorkbench workbench, String itemKey) {
        return workbench.getItems().stream()
                .filter(item -> itemKey.equals(item.getItemKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing work item: " + itemKey));
    }
}
