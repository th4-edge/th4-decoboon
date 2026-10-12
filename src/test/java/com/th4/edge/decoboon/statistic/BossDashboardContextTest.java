package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.statistic.context.BossDashboardContext;
import com.th4.edge.decoboon.statistic.context.StatisticStoreAggregateContext;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossDashboardQuery;
import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.decoboon.statistic.repository.BusinessTargetRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 驾驶舱指标组装（P0B-02 聚合下推后）：
 * 标量（签约额 / 收款额 / 在建项目数）来自 {@link StatisticStoreAggregateContext} 的 **SQL 聚合**，
 * 毛利按 `BR-031` 业务公式在内存计算（批量取数、循环内零查询）。本类只验证指标组装与
 * 达成率 / 环比 / 同比换算，SQL 侧口径由 {@code StatisticStoreAggregateContextTest} 覆盖。
 */
class BossDashboardContextTest {

    private final TenantContext tenant = mock(TenantContext.class);
    private final ProjectContractRepository contracts = mock(ProjectContractRepository.class);
    private final BusinessTargetRepository targets = mock(BusinessTargetRepository.class);
    private final ProjectCostContext costs = mock(ProjectCostContext.class);
    private final StatisticStoreAggregateContext aggregate = mock(StatisticStoreAggregateContext.class);

    private final BossDashboardContext context =
            new BossDashboardContext(tenant, contracts, targets, costs, aggregate);

    private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);

    @Test
    void shouldAssembleMetricsFromSqlAggregatesTargetsAndComparisons() {
        Company company = new Company();
        company.setComId(10L);
        Employee boss = new Employee();
        boss.setEmpId(11L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);

        Project project = new Project();
        project.setProjectId(20L);
        project.setName("当前项目");
        ProjectContract contract = new ProjectContract();
        contract.setProject(project);
        contract.setBusinessOrigin(BusinessOrigin.New);
        contract.setCurrentAmount(new BigDecimal("10000.00"));
        contract.setBaselineAmount(new BigDecimal("10000.00"));
        contract.setSignedAt(MONTH.plusDays(3));

        BusinessTarget target = new BusinessTarget();
        target.setTargetId(1L);
        target.setSignedAmount(new BigDecimal("12000.00"));

        when(tenant.requireBoss(boss)).thenReturn(boss);
        when(targets.findByCompany_ComIdAndTargetMonth(10L, MONTH)).thenReturn(Optional.of(target));
        when(aggregate.signedContractAmount(eq(10L), anyLong(), eq(MONTH))).thenReturn(new BigDecimal("10000.00"));
        when(aggregate.signedContractAmount(eq(10L), anyLong(), eq(MONTH.minusMonths(1))))
                .thenReturn(new BigDecimal("5000.00"));
        when(aggregate.signedContractAmount(eq(10L), anyLong(), eq(MONTH.minusYears(1))))
                .thenReturn(BigDecimal.ZERO);
        when(aggregate.receiptAmount(eq(10L), anyLong(), eq(MONTH))).thenReturn(new BigDecimal("3000.00"));
        when(aggregate.receiptAmount(eq(10L), anyLong(), eq(MONTH.minusMonths(1))))
                .thenReturn(new BigDecimal("1500.00"));
        when(aggregate.receiptAmount(eq(10L), anyLong(), eq(MONTH.minusYears(1)))).thenReturn(BigDecimal.ZERO);
        when(aggregate.activeProjectCount(eq(10L), anyLong(), eq(MONTH))).thenReturn(2L);
        when(aggregate.activeProjectCount(eq(10L), anyLong(), eq(MONTH.minusMonths(1)))).thenReturn(1L);
        when(aggregate.activeProjectCount(eq(10L), anyLong(), eq(MONTH.minusYears(1)))).thenReturn(0L);
        when(contracts.findNormalSignedBetweenByStoreScope(eq(10L), any(), any(), any(), anyBoolean(), anyLong()))
                .thenReturn(List.of(contract));
        when(aggregate.payableAmountByProject(eq(10L), anyLong(), any()))
                .thenReturn(Map.of(20L, new BigDecimal("4000.00")));
        when(costs.summarizeAll(any())).thenReturn(Map.of(20L,
                new ProjectCostContext.CostSummary(new BigDecimal("4000.00"), new BigDecimal("4000.00"), 0)));

        BossDashboardQuery query = new BossDashboardQuery();
        query.setMonth(MONTH);
        BossDashboard dashboard = context.dashboard(boss, query);

        assertThat(dashboard.getSignedAmount().getValue()).isEqualByComparingTo("10000.00");
        assertThat(dashboard.getSignedAmount().getTarget()).isEqualByComparingTo("12000.00");
        assertThat(dashboard.getSignedAmount().getAchievementRate()).isEqualByComparingTo("83.3");
        assertThat(dashboard.getSignedAmount().getMonthOverMonthRate()).isEqualByComparingTo("100.0");
        assertThat(dashboard.getReceivedAmount().getValue()).isEqualByComparingTo("3000.00");
        assertThat(dashboard.getGrossProfit().getValue()).isEqualByComparingTo("6000.00");
        assertThat(dashboard.getGrossProfit().getComplete()).isTrue();
        assertThat(dashboard.getActiveProjectCount().getValue()).isEqualByComparingTo("2");
    }

    @Test
    void shouldReturnNullProfitWhenAnyProjectCostIsIncomplete() {
        Company company = new Company();
        company.setComId(10L);
        Employee boss = new Employee();
        boss.setEmpId(11L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);

        Project project = new Project();
        project.setProjectId(20L);
        ProjectContract contract = new ProjectContract();
        contract.setProject(project);
        contract.setBusinessOrigin(BusinessOrigin.New);
        contract.setCurrentAmount(new BigDecimal("10000.00"));
        contract.setSignedAt(MONTH.plusDays(3));

        when(tenant.requireBoss(boss)).thenReturn(boss);
        when(targets.findByCompany_ComIdAndTargetMonth(10L, MONTH)).thenReturn(Optional.empty());
        when(aggregate.signedContractAmount(eq(10L), anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(aggregate.receiptAmount(eq(10L), anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(aggregate.activeProjectCount(eq(10L), anyLong(), any())).thenReturn(0L);
        when(contracts.findNormalSignedBetweenByStoreScope(eq(10L), any(), any(), any(), anyBoolean(), anyLong()))
                .thenReturn(List.of(contract));
        when(aggregate.payableAmountByProject(eq(10L), anyLong(), any())).thenReturn(Map.of());
        when(costs.summarizeAll(any())).thenReturn(Map.of(20L,
                new ProjectCostContext.CostSummary(new BigDecimal("4000.00"), new BigDecimal("4000.00"), 1)));

        BossDashboardQuery query = new BossDashboardQuery();
        query.setMonth(MONTH);
        BossDashboard dashboard = context.dashboard(boss, query);

        assertThat(dashboard.getGrossProfit().getComplete()).isFalse();
        assertThat(dashboard.getGrossProfit().getValue()).isNull();
        assertThat(dashboard.getGrossProfit().getWarning()).contains("成本未完整");
        // 不完整时达成率/环比/同比一律不展示（不得用 0 成本算伪利润）
        assertThat(dashboard.getGrossProfit().getAchievementRate()).isEqualByComparingTo("0");
        assertThat(dashboard.getGrossProfit().getMonthOverMonthComparable()).isFalse();
        assertThat(dashboard.getGrossProfit().getYearOverYearComparable()).isFalse();
    }
}
