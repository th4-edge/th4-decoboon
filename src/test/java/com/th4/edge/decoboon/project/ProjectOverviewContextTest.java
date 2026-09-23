package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectContractStage;
import com.th4.edge.decoboon.project.configure.ProjectRiskLevel;
import com.th4.edge.decoboon.project.configure.ProjectRiskType;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.context.ProjectOverviewContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectMetricCard;
import com.th4.edge.decoboon.project.model.ProjectOverviewReport;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.context.BusinessAlertContext;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 企业负责人项目 Tab 经营聚合：
 * 四张指标卡（含本月新签项目）、双维度分段、风险投影、实际成本毛利口径与排序。
 */
class ProjectOverviewContextTest {

    private static final long COM_ID = 7L;

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
    private final ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
    private final ProjectStageRepository stageRepository = mock(ProjectStageRepository.class);
    private final ProjectCostContext costContext = mock(ProjectCostContext.class);
    private final BusinessAlertContext alertContext = mock(BusinessAlertContext.class);

    private final ProjectOverviewContext context = new ProjectOverviewContext(tenantContext, projectRepository,
            contractRepository, receivableRepository, stageRepository, costContext, alertContext);

    private Employee boss;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(COM_ID);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        when(tenantContext.requireBoss(any())).thenReturn(boss);
        when(stageRepository.findByProjectOrderBySort(any())).thenReturn(List.of());
    }

    @Test
    void shouldAggregateOperatingIndicatorsAndProjectRiskProjection() {
        Project suspended = project(101L, "滨湖云谷王总", ProjectStatus.Suspended, LocalDate.now().minusDays(3));
        Project ongoing = project(102L, "政务区李总", ProjectStatus.Ongoing, LocalDate.now().plusDays(20));
        Project finished = project(103L, "蜀山区赵总", ProjectStatus.Finished, LocalDate.now().minusDays(40));

        mockProjects(List.of(suspended, ongoing, finished));
        mockContracts(
                contract(suspended, "100000", ContractStatus.Effective, LocalDate.now()),
                contract(ongoing, "200000", ContractStatus.Effective, LocalDate.now().minusMonths(2)),
                contract(finished, "50000", ContractStatus.Settled, LocalDate.now().minusMonths(5)));
        mockReceivables(
                receivable(suspended, "100000", "20000", ReceivableStatus.Overdue, LocalDate.now().minusDays(12)),
                receivable(ongoing, "200000", "180000", ReceivableStatus.Partial, LocalDate.now().plusDays(10)),
                receivable(finished, "50000", "50000", ReceivableStatus.Received, LocalDate.now().minusDays(35)));
        mockAlerts(alert(suspended, BusinessAlertType.ProjectSuspensionRisk, BusinessAlertLevel.High,
                "滨湖云谷王总 当前处于停工状态，请核查原因和恢复计划"));

        when(costContext.summarize(suspended))
                .thenReturn(new ProjectCostContext.CostSummary(BigDecimal.valueOf(62000), BigDecimal.valueOf(62000), 2));
        when(costContext.summarize(ongoing))
                .thenReturn(new ProjectCostContext.CostSummary(BigDecimal.valueOf(160000), BigDecimal.valueOf(160000), 0));
        when(costContext.summarize(finished))
                .thenReturn(new ProjectCostContext.CostSummary(BigDecimal.valueOf(40000), BigDecimal.valueOf(40000), 0));

        ProjectOverviewReport report = context.report(boss, 1, 20, null, null, null, null, null, null);

        assertThat(report.getAsOf()).isEqualTo(LocalDate.now());
        Map<String, ProjectMetricCard> metrics = report.getMetrics().stream()
                .collect(Collectors.toMap(ProjectMetricCard::getKey, Function.identity()));
        assertThat(metrics.keySet()).containsExactlyInAnyOrder(
                "activeProject", "activeContractAmount", "receivedRate", "newSignedProject");

        // 在建 = 待开工 + 施工中 + 停工 + 待验收，已完工不计入
        assertThat(metrics.get("activeProject").getValue()).isEqualByComparingTo("2");
        assertThat(metrics.get("activeProject").getStatus()).isEqualTo("danger");
        assertThat(metrics.get("activeProject").getSubText()).contains("施工中 1");
        assertThat(metrics.get("activeContractAmount").getValue()).isEqualByComparingTo("300000");
        // 回款率 = 已收 200000 / 应收 300000，且存在逾期应收 -> danger
        assertThat(metrics.get("receivedRate").getValue()).isEqualByComparingTo("66.7");
        assertThat(metrics.get("receivedRate").getStatus()).isEqualTo("danger");
        // 第四张卡：本月新签项目（本月签约 1 个，上月 0 个 -> 不制造无穷增长，趋势为空）
        assertThat(metrics.get("newSignedProject").getValue()).isEqualByComparingTo("1");
        assertThat(metrics.get("newSignedProject").getSubText()).contains("上月 0 个").contains("本月签约额");
        assertThat(metrics.get("newSignedProject").getTrendRate()).isNull();

        // 风险投影复用经营预警口径（风险不再单独占指标卡）
        assertThat(suspended.getRiskLevel()).isEqualTo(ProjectRiskLevel.High);
        assertThat(suspended.getRiskCount()).isEqualTo(1);
        assertThat(suspended.getMainRiskType()).isEqualTo(ProjectRiskType.DeliveryRisk);
        assertThat(suspended.getNextAction()).contains("复工");
        assertThat(suspended.getReceivedAmount()).isEqualByComparingTo("20000");
        assertThat(suspended.getReceivableAmount()).isEqualByComparingTo("100000");
        assertThat(suspended.getOverdueReceivableCount()).isEqualTo(1);
        assertThat(suspended.getOverdueDays()).isEqualTo(3);
        // 无风险项目保持中性，不制造伪预警
        assertThat(ongoing.getRiskLevel()).isEqualTo(ProjectRiskLevel.Normal);
        assertThat(ongoing.getRiskCount()).isZero();
        assertThat(ongoing.getRiskReason()).isNull();
        assertThat(ongoing.getNextAction()).contains("施工");

        // 实际成本口径：成本不完整 -> 毛利为空；成本完整 -> 合同额 - 实际成本
        assertThat(suspended.getCostComplete()).isFalse();
        assertThat(suspended.getActualCost()).isEqualByComparingTo("62000");
        assertThat(suspended.getGrossProfit()).isNull();
        // 成本不完整时毛利率同样必须为空，禁止 0 成本伪利润
        assertThat(suspended.getGrossMargin()).isNull();
        assertThat(ongoing.getCostComplete()).isTrue();
        assertThat(ongoing.getActualCost()).isEqualByComparingTo("160000");
        assertThat(ongoing.getGrossProfit()).isEqualByComparingTo("40000");
        // 毛利率 = (合同额 - 实际成本) / 合同额 = 40000 / 200000 = 20.0%
        assertThat(ongoing.getGrossMargin()).isEqualByComparingTo("20.0");

        // 合同履约阶段投影
        assertThat(suspended.getContractStage()).isEqualTo(ProjectContractStage.Active);
        assertThat(finished.getContractStage()).isEqualTo(ProjectContractStage.Completed);

        assertThat(report.getStatusCounts()).extracting("key", "count")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("all", 3),
                        org.assertj.core.groups.Tuple.tuple("preparing", 0),
                        org.assertj.core.groups.Tuple.tuple("ongoing", 1),
                        org.assertj.core.groups.Tuple.tuple("suspended", 1),
                        org.assertj.core.groups.Tuple.tuple("accepting", 0),
                        org.assertj.core.groups.Tuple.tuple("finished", 1));
        assertThat(report.getContractCounts()).extracting("key", "count")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("all", 3),
                        org.assertj.core.groups.Tuple.tuple("pending", 0),
                        org.assertj.core.groups.Tuple.tuple("active", 2),
                        org.assertj.core.groups.Tuple.tuple("completed", 1));
        assertThat(report.getTotal()).isEqualTo(3);
        assertThat(report.getHasMore()).isFalse();
    }

    @Test
    void shouldSortByReceivedRateAndPageResults() {
        Project slow = project(201L, "回款最慢项目", ProjectStatus.Ongoing, LocalDate.now().plusDays(30));
        Project medium = project(202L, "回款中等项目", ProjectStatus.Ongoing, LocalDate.now().plusDays(30));
        Project noPlan = project(203L, "未生成应收项目", ProjectStatus.Ongoing, LocalDate.now().plusDays(30));

        mockProjects(List.of(noPlan, medium, slow));
        mockContracts(
                contract(slow, "100000", ContractStatus.Effective, null),
                contract(medium, "100000", ContractStatus.Effective, null),
                contract(noPlan, "100000", ContractStatus.Effective, null));
        mockReceivables(
                receivable(slow, "100000", "10000", ReceivableStatus.Partial, LocalDate.now().plusDays(5)),
                receivable(medium, "100000", "55000", ReceivableStatus.Partial, LocalDate.now().plusDays(5)));
        mockAlerts();
        when(costContext.summarize(any()))
                .thenReturn(new ProjectCostContext.CostSummary(BigDecimal.ZERO, BigDecimal.ZERO, 1));

        ProjectOverviewReport report = context.report(boss, 1, 2, null, null, null, null, "received", null);

        // 回款最慢优先：10% -> 55% -> 未生成应收计划排最后
        assertThat(report.getProjects()).extracting(Project::getProjectId).containsExactly(201L, 202L);
        assertThat(report.getTotal()).isEqualTo(3);
        assertThat(report.getHasMore()).isTrue();

        ProjectOverviewReport second = context.report(boss, 2, 2, null, null, null, null, "received", null);
        assertThat(second.getProjects()).extracting(Project::getProjectId).containsExactly(203L);
        assertThat(second.getHasMore()).isFalse();
    }

    @Test
    void shouldFilterByStatusAndContractStage() {
        Project preparing = project(301L, "待开工项目", ProjectStatus.Preparing, LocalDate.now().plusDays(10));
        Project ongoing = project(302L, "施工中项目", ProjectStatus.Ongoing, LocalDate.now().plusDays(10));
        Project finished = project(303L, "已完工项目", ProjectStatus.Finished, LocalDate.now().minusDays(10));

        mockProjects(List.of(preparing, ongoing, finished));
        mockContracts(
                contract(preparing, "80000", ContractStatus.PendingSign, null),
                contract(ongoing, "90000", ContractStatus.Effective, LocalDate.now().minusMonths(1)),
                contract(finished, "70000", ContractStatus.Completed, LocalDate.now().minusMonths(6)));
        mockReceivables();
        mockAlerts();
        when(costContext.summarize(any()))
                .thenReturn(new ProjectCostContext.CostSummary(BigDecimal.ZERO, BigDecimal.ZERO, 1));

        ProjectOverviewReport ongoingOnly = context.report(boss, 1, 20, "ongoing", null, null, null, null, null);
        assertThat(ongoingOnly.getProjects()).extracting(Project::getProjectId).containsExactly(302L);
        assertThat(ongoingOnly.getTotal()).isEqualTo(1);

        ProjectOverviewReport active = context.report(boss, 1, 20, null, "active", null, null, null, null);
        assertThat(active.getProjects()).extracting(Project::getProjectId).containsExactly(302L);

        ProjectOverviewReport pending = context.report(boss, 1, 20, null, "pending", null, null, null, null);
        assertThat(pending.getProjects()).extracting(Project::getProjectId).containsExactly(301L);

        ProjectOverviewReport completed = context.report(boss, 1, 20, null, "completed", null, null, null, null);
        assertThat(completed.getProjects()).extracting(Project::getProjectId).containsExactly(303L);

        // 合同履约分段计数与「全部」自洽
        assertThat(completed.getContractCounts()).extracting("key", "count")
                .contains(org.assertj.core.groups.Tuple.tuple("all", 3),
                        org.assertj.core.groups.Tuple.tuple("pending", 1),
                        org.assertj.core.groups.Tuple.tuple("active", 1),
                        org.assertj.core.groups.Tuple.tuple("completed", 1));
    }

    @Test
    void shouldReportMonthlySigningTrend() {
        Project thisMonth = project(401L, "本月签约项目", ProjectStatus.Preparing, LocalDate.now().plusDays(20));
        Project lastMonth = project(402L, "上月签约项目", ProjectStatus.Ongoing, LocalDate.now().plusDays(20));

        mockProjects(List.of(thisMonth, lastMonth));
        mockContracts(
                contract(thisMonth, "120000", ContractStatus.Effective, LocalDate.now()),
                contract(lastMonth, "80000", ContractStatus.Effective, LocalDate.now().minusMonths(1)));
        mockReceivables();
        mockAlerts();
        when(costContext.summarize(any()))
                .thenReturn(new ProjectCostContext.CostSummary(BigDecimal.ZERO, BigDecimal.ZERO, 1));

        ProjectOverviewReport report = context.report(boss, 1, 20, null, null, null, null, null, null);
        ProjectMetricCard card = report.getMetrics().stream()
                .filter(item -> "newSignedProject".equals(item.getKey()))
                .findFirst()
                .orElseThrow();

        assertThat(card.getValue()).isEqualByComparingTo("1");
        assertThat(card.getSubText()).contains("上月 1 个").contains("120000");
        assertThat(card.getTrendRate()).isEqualByComparingTo("0");
    }

    @Test
    void shouldFilterByRiskLevelForRiskSegment() {
        Project high = project(501L, "高层风险项目", ProjectStatus.Suspended, LocalDate.now().minusDays(3));
        Project normal = project(502L, "正常项目", ProjectStatus.Ongoing, LocalDate.now().plusDays(20));
        Project attention = project(503L, "需关注项目", ProjectStatus.Ongoing, LocalDate.now().plusDays(10));

        mockProjects(List.of(high, normal, attention));
        mockContracts(
                contract(high, "300000", ContractStatus.Effective, LocalDate.now()),
                contract(normal, "200000", ContractStatus.Effective, LocalDate.now()),
                contract(attention, "100000", ContractStatus.Effective, LocalDate.now()));
        mockReceivables();
        mockAlerts(
                alert(high, BusinessAlertType.ProjectSuspensionRisk, BusinessAlertLevel.High, "已停工"),
                alert(attention, BusinessAlertType.AbnormalChangeRate, BusinessAlertLevel.Medium, "变更率异常"));
        when(costContext.summarize(any()))
                .thenReturn(new ProjectCostContext.CostSummary(BigDecimal.valueOf(50000),
                        BigDecimal.valueOf(50000), 0));

        // 无筛选：三个项目都在
        ProjectOverviewReport all = context.report(boss, 1, 20, null, null, null, null, null, null);
        assertThat(all.getProjects()).extracting(Project::getProjectId)
                .containsExactlyInAnyOrder(501L, 502L, 503L);

        // UI「有风险」分段：只保留 riskLevel != Normal
        ProjectOverviewReport riskOnly = context.report(boss, 1, 20, null, null, "risk", null, null, null);
        assertThat(riskOnly.getProjects()).extracting(Project::getProjectId)
                .containsExactlyInAnyOrder(501L, 503L);

        // 按等级精确筛选
        ProjectOverviewReport highOnly = context.report(boss, 1, 20, null, null, "High", null, null, null);
        assertThat(highOnly.getProjects()).extracting(Project::getProjectId).containsExactly(501L);

        ProjectOverviewReport normalOnly = context.report(boss, 1, 20, null, null, "Normal", null, null, null);
        assertThat(normalOnly.getProjects()).extracting(Project::getProjectId).containsExactly(502L);

        // 指标与分段计数不受列表筛选影响（仍为全企业口径）
        assertThat(all.getMetrics()).extracting(ProjectMetricCard::getKey)
                .contains("activeProject", "activeContractAmount", "receivedRate", "newSignedProject");
    }

    // ------------------------------------------------------------------ 构造

    private void mockProjects(List<Project> projects) {
        when(projectRepository.findByCompany_ComIdOrderByProjectIdDesc(COM_ID)).thenReturn(projects);
    }

    private void mockContracts(ProjectContract... contracts) {
        when(contractRepository.findByCompany_ComIdOrderByContractIdDesc(COM_ID)).thenReturn(List.of(contracts));
    }

    private void mockReceivables(Receivable... receivables) {
        when(receivableRepository.findByCompany_ComIdOrderByDueDateAsc(COM_ID)).thenReturn(List.of(receivables));
    }

    private void mockAlerts(BusinessAlert... alerts) {
        BusinessAlertReport report = new BusinessAlertReport();
        report.setAsOf(LocalDate.now());
        report.setSuspensionRisks(List.of(alerts));
        report.setOverdueReceivables(List.of());
        report.setMaterialOverIssues(List.of());
        report.setChangeAnomalies(List.of());
        when(alertContext.report(any(), any())).thenReturn(report);
    }

    private Project project(Long id, String name, ProjectStatus status, LocalDate planEndDate) {
        Project project = new Project();
        project.setProjectId(id);
        project.setNo("P-" + id);
        project.setName(name);
        project.setStatus(status);
        project.setProgress(50);
        project.setArea(BigDecimal.valueOf(120));
        project.setSiteAddr("合肥市测试路 " + id + " 号");
        project.setPlanEndDate(planEndDate);
        return project;
    }

    private ProjectContract contract(Project project, String currentAmount, ContractStatus status, LocalDate signedAt) {
        ProjectContract contract = new ProjectContract();
        contract.setProject(project);
        contract.setCurrentAmount(new BigDecimal(currentAmount));
        contract.setBaselineAmount(new BigDecimal(currentAmount));
        contract.setStatus(status);
        contract.setSignedAt(signedAt);
        return contract;
    }

    private Receivable receivable(Project project, String amount, String received,
                                 ReceivableStatus status, LocalDate dueDate) {
        Receivable receivable = new Receivable();
        receivable.setProject(project);
        receivable.setAmount(new BigDecimal(amount));
        receivable.setReceivedAmount(new BigDecimal(received));
        receivable.setStatus(status);
        receivable.setDueDate(dueDate);
        return receivable;
    }

    private BusinessAlert alert(Project project, BusinessAlertType type, BusinessAlertLevel level, String message) {
        BusinessAlert alert = new BusinessAlert();
        alert.setType(type);
        alert.setLevel(level);
        alert.setProject(project);
        alert.setMessage(message);
        alert.setBusinessType("Project");
        alert.setBusinessId(project.getProjectId());
        return alert;
    }
}
