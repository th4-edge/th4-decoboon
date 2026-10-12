package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.common.context.BusinessOriginFilter;

import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.StoreScopeInfo;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.decoboon.statistic.model.BossDashboardQuery;
import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.decoboon.statistic.model.DashboardMetric;
import com.th4.edge.decoboon.statistic.repository.BusinessTargetRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 企业负责人驾驶舱指标计算。 */
@Component
public class BossDashboardContext {

    private final TenantContext tenantContext;
    private final ProjectContractRepository contractRepository;
    private final BusinessTargetRepository targetRepository;
    private final ProjectCostContext projectCostContext;
    private final StatisticStoreAggregateContext aggregateContext;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.th4.edge.decoboon.finance.context.ManualLedgerContext manualLedgerContext;

    /** 门店维度范围标注所需（范围标注为本批新增的只读补充，不改口径）。 */
    @org.springframework.beans.factory.annotation.Autowired
    private StoreRepository storeRepository;

    public BossDashboardContext(TenantContext tenantContext,
                                ProjectContractRepository contractRepository,
                                BusinessTargetRepository targetRepository,
                                ProjectCostContext projectCostContext,
                                StatisticStoreAggregateContext aggregateContext) {
        this.tenantContext = tenantContext;
        this.contractRepository = contractRepository;
        this.targetRepository = targetRepository;
        this.projectCostContext = projectCostContext;
        this.aggregateContext = aggregateContext;
    }

    public BossDashboard dashboard(Employee principal, BossDashboardQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        LocalDate month = query == null || query.getMonth() == null
                ? LocalDate.now().withDayOfMonth(1)
                : query.getMonth().withDayOfMonth(1);
        LocalDate previousMonth = month.minusMonths(1);
        LocalDate previousYear = month.minusYears(1);

        // 门店维度（DEC-067）：不传 / null / 0 等价为全部门店；单店视图不含未归属门店记录。
        // P0B-02 聚合下推：门店条件与全部标量（签约额/收款额/在建项目数）都在 SQL 侧完成，
        // 不再「全量加载 + 内存 filter/sum」（DEC-036 / BR-031）。
        Long storeFilter = StoreScope.normalize(query == null ? null : query.getStoreId());
        BusinessTarget target = targetRepository.findByCompany_ComIdAndTargetMonth(comId, month)
                .orElseGet(BusinessTarget::new);
        boolean targetSet = target.getTargetId() != null;

        BigDecimal signed = aggregateContext.signedContractAmount(comId, storeFilter, month);
        BigDecimal previousMonthSigned = aggregateContext.signedContractAmount(comId, storeFilter, previousMonth);
        BigDecimal previousYearSigned = aggregateContext.signedContractAmount(comId, storeFilter, previousYear);
        BigDecimal received = aggregateContext.receiptAmount(comId, storeFilter, month);
        BigDecimal previousMonthReceived = aggregateContext.receiptAmount(comId, storeFilter, previousMonth);
        BigDecimal previousYearReceived = aggregateContext.receiptAmount(comId, storeFilter, previousYear);
        ProfitResult profit = grossProfit(comId, storeFilter, month);
        ProfitResult previousMonthProfit = grossProfit(comId, storeFilter, previousMonth);
        ProfitResult previousYearProfit = grossProfit(comId, storeFilter, previousYear);
        BigDecimal active = BigDecimal.valueOf(aggregateContext.activeProjectCount(comId, storeFilter, month));
        BigDecimal previousMonthActive = BigDecimal.valueOf(aggregateContext.activeProjectCount(comId, storeFilter, previousMonth));
        BigDecimal previousYearActive = BigDecimal.valueOf(aggregateContext.activeProjectCount(comId, storeFilter, previousYear));

        BossDashboard dashboard = new BossDashboard();
        dashboard.setMonth(month);
        dashboard.setSignedAmount(metric(signed, target.getSignedAmount(), previousMonthSigned, previousYearSigned, targetSet));
        dashboard.setReceivedAmount(metric(received, target.getReceivedAmount(), previousMonthReceived, previousYearReceived, targetSet));
        DashboardMetric profitMetric = metric(profit.value(), target.getGrossProfit(),
                previousMonthProfit.value(), previousYearProfit.value(), targetSet);
        profitMetric.setComplete(profit.complete());
        profitMetric.setWarning(profit.warning());
        if (!profit.complete()) {
            profitMetric.setValue(null);
            profitMetric.setAchievementRate(BigDecimal.ZERO);
            profitMetric.setMonthOverMonthRate(BigDecimal.ZERO);
            profitMetric.setYearOverYearRate(BigDecimal.ZERO);
            profitMetric.setMonthOverMonthComparable(Boolean.FALSE);
            profitMetric.setYearOverYearComparable(Boolean.FALSE);
        }
        dashboard.setGrossProfit(profitMetric);
        dashboard.setActiveProjectCount(metric(active, BigDecimal.valueOf(value(target.getActiveProjectCount())),
                previousMonthActive, previousYearActive, targetSet));
        dashboard.setStoreScope(storeScopeInfo(comId, storeFilter));
        return dashboard;
    }

    /** 门店维度范围标注（`DEC-067` BOSS-STORE-03）：未归属条数由 SQL `COUNT` 提供，单店视图恒 0。 */
    private StoreScopeInfo storeScopeInfo(Long comId, Long storeFilter) {
        StoreScopeInfo info = new StoreScopeInfo();
        boolean allStores = StoreScope.isAllStores(storeFilter);
        info.setScopeType(allStores ? "AllStores" : "Store");
        if (!allStores) {
            info.setStoreId(storeFilter);
            Store store = storeRepository.findByStoreId(storeFilter);
            if (store != null) {
                info.setStoreName(store.getName());
            }
            return info;
        }
        info.setUnassignedCount((int) unassignedCount(comId));
        return info;
    }

    private long unassignedCount(Long comId) {
        return aggregateContext.unassignedCustomers(comId);
    }

    private DashboardMetric metric(BigDecimal value, BigDecimal target, BigDecimal previousMonth, BigDecimal previousYear, boolean targetSet) {
        DashboardMetric metric = new DashboardMetric();
        metric.setValue(value);
        metric.setTarget(value(target));
        metric.setTargetSet(targetSet);
        metric.setAchievementRate(percentage(value, value(target)));
        metric.setPreviousMonthValue(value(previousMonth));
        metric.setPreviousYearValue(value(previousYear));
        metric.setMonthOverMonthComparable(previousMonth != null && previousMonth.signum() != 0);
        metric.setYearOverYearComparable(previousYear != null && previousYear.signum() != 0);
        metric.setMonthOverMonthRate(changeRate(value, value(previousMonth)));
        metric.setYearOverYearRate(changeRate(value, value(previousYear)));
        return metric;
    }

    /**
     * 毛利公式（`BR-031` 第二类：成本/毛利类业务公式允许内存计算，但**循环体内零查询**）。
     *
     * <p>取数全部批量：本月合同一次 SQL（区间 + 来源 + 门店条件下推）；应付按项目一次 `GROUP BY`；
     * 成本完整性用 {@link ProjectCostContext#summarizeAll}（批量）；手工账用
     * {@link com.th4.edge.decoboon.finance.context.ManualLedgerContext#amountsAll}（批量）。</p>
     */
    private ProfitResult grossProfit(Long comId, Long storeFilter, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        List<ProjectContract> contracts = contractRepository.findNormalSignedBetweenByStoreScope(
                comId, from, to, BusinessOrigin.New, StoreScope.isAllStores(storeFilter), storeFilter);
        if (contracts.isEmpty()) {
            return new ProfitResult(BigDecimal.ZERO, true, null);
        }
        List<Project> projects = contracts.stream().map(ProjectContract::getProject)
                .filter(project -> project != null && project.getProjectId() != null)
                .collect(java.util.stream.Collectors.toMap(Project::getProjectId, project -> project,
                        (left, right) -> left, LinkedHashMap::new))
                .values().stream().toList();
        Map<Long, BigDecimal> costByProject = aggregateContext.payableAmountByProject(comId, storeFilter,
                projects.stream().map(Project::getProjectId).toList());
        Map<Long, ProjectCostContext.CostSummary> costSummaries = projectCostContext.summarizeAll(projects);
        Map<Long, com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts> manualByProject =
                manualLedgerContext == null
                        ? Map.of()
                        : manualLedgerContext.amountsAll(projects);

        return profitOf(contracts, month, costByProject, costSummaries, manualByProject);
    }

    /**
     * 毛利口径（**唯一实现**，供经营总览与门店对比矩阵共用；`P0D-05`）。
     *
     * <p>逐合同：非正常经营来源跳过、非本月签约跳过；成本取「实际成本 vs 采购应付」较大者 + 手工账成本；
     * 任一项目成本不完整 → 整体不完整（返回 {@code null} + 提示，禁止展示伪利润）。</p>
     */
    public static ProfitResult profitOf(List<ProjectContract> contracts, LocalDate month,
                                        Map<Long, BigDecimal> costByProject,
                                        Map<Long, ProjectCostContext.CostSummary> costSummaries,
                                        Map<Long, com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts> manualByProject) {
        BigDecimal profit = BigDecimal.ZERO;
        int incomplete = 0;
        for (ProjectContract contract : contracts) {
            if (!BusinessOriginFilter.isNormal(contract.getBusinessOrigin())) {
                continue;
            }
            if (!sameMonthStatic(contract.getSignedAt(), month)) {
                continue;
            }
            Project project = contract.getProject();
            Long projectId = project == null ? null : project.getProjectId();
            BigDecimal payableCost = projectId == null
                    ? BigDecimal.ZERO
                    : costByProject.getOrDefault(projectId, BigDecimal.ZERO);
            ProjectCostContext.CostSummary costSummary = projectId == null ? null : costSummaries.get(projectId);
            com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts manual = projectId == null
                    ? com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts.ZERO
                    : manualByProject.getOrDefault(projectId,
                            com.th4.edge.decoboon.finance.context.ManualLedgerContext.ManualAmounts.ZERO);
            if (costSummary == null || !costSummary.complete()) {
                incomplete++;
                continue;
            }
            BigDecimal cost = costSummary.actualCost().max(payableCost).add(manual.cost());
            profit = profit.add(contractRevenueStatic(contract).add(manual.income()).subtract(cost));
        }
        if (incomplete > 0) {
            return new ProfitResult(null, false, incomplete + " 个项目的成本未完整，暂不展示可靠毛利");
        }
        return new ProfitResult(profit, true, null);
    }

    private static boolean sameMonthStatic(LocalDate date, LocalDate month) {
        return date != null && YearMonth.from(date).equals(YearMonth.from(month));
    }

    private static BigDecimal contractRevenueStatic(ProjectContract contract) {
        BigDecimal current = contract.getCurrentAmount() == null ? BigDecimal.ZERO : contract.getCurrentAmount();
        return current.signum() == 0
                ? (contract.getBaselineAmount() == null ? BigDecimal.ZERO : contract.getBaselineAmount())
                : current;
    }

    public record ProfitResult(BigDecimal value, boolean complete, String warning) {}


    private BigDecimal contractRevenue(ProjectContract contract) {
        return value(contract.getCurrentAmount()).signum() == 0
                ? value(contract.getBaselineAmount()) : value(contract.getCurrentAmount());
    }

    private boolean sameMonth(LocalDate date, LocalDate month) {
        return date != null && YearMonth.from(date).equals(YearMonth.from(month));
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value(numerator).multiply(BigDecimal.valueOf(100))
                .divide(denominator, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal changeRate(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

}
