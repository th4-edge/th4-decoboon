package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.statistic.context.OrganizationContext;
import com.th4.edge.decoboon.statistic.context.PerformanceReportContext;
import com.th4.edge.decoboon.statistic.context.StatisticStoreAggregateContext;
import com.th4.edge.decoboon.statistic.model.EmployeePerformance;
import com.th4.edge.decoboon.statistic.model.OrganizationQuery;
import com.th4.edge.decoboon.statistic.model.OrganizationReport;
import com.th4.edge.decoboon.statistic.model.PerformanceReport;
import com.th4.edge.decoboon.statistic.model.RoleCapacity;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 组织效能聚合（P0B-02 聚合下推后）：
 * 在职/入职/角色在编、签约额、新项目数来自 SQL 聚合；角色绩效均值与排行仍复用唯一实现
 * {@link PerformanceReportContext}。本类验证组织效能的组装、人均口径与排行。
 */
class OrganizationContextTest {

    private static final long COM_ID = 21L;
    private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final PerformanceReportContext performanceContext = mock(PerformanceReportContext.class);
    private final StatisticStoreAggregateContext aggregate = mock(StatisticStoreAggregateContext.class);

    private final OrganizationContext context =
            new OrganizationContext(tenantContext, storeRepository, performanceContext, aggregate);

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
    }

    @Test
    void shouldAssembleHeadcountPerCapitaAndRoleCapacityFromSqlAggregates() {
        Map<Object, Long> roleCounts = new LinkedHashMap<>();
        roleCounts.put(EmployeeRole.Designer, 2L);
        roleCounts.put(EmployeeRole.Marketer, 1L);
        roleCounts.put(EmployeeRole.Pm, 1L);

        when(aggregate.organizationCounts(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(new StatisticStoreAggregateContext.OrganizationCounts(4, 1, roleCounts));
        when(aggregate.organizationSignedAmount(eq(COM_ID), anyLong(), eq(MONTH)))
                .thenReturn(new BigDecimal("400000"));
        when(aggregate.organizationNewProjectCount(eq(COM_ID), anyLong(), eq(MONTH))).thenReturn(2L);
        when(aggregate.unassignedEmployees(COM_ID)).thenReturn(0L);
        when(performanceContext.report(any(), any())).thenReturn(performance());

        OrganizationReport report = context.report(boss, query(MONTH, null));

        assertThat(report.getMonth()).isEqualTo(MONTH);
        assertThat(report.getStoreScope().getScopeType()).isEqualTo("AllStores");
        assertThat(report.getStoreScope().getUnassignedCount()).isZero();
        assertThat(report.getHeadcount()).isEqualTo(4);
        assertThat(report.getNewHires()).isEqualTo(1);
        assertThat(report.getSignedAmount()).isEqualByComparingTo("400000");
        assertThat(report.getNewProjectCount()).isEqualTo(2);
        assertThat(report.getPerCapitaOutput()).isEqualByComparingTo("100000.00");
        assertThat(report.getPerCapitaOrders()).isEqualByComparingTo("0.50");

        Map<String, RoleCapacity> capacities = report.getRoleCapacities().stream()
                .collect(Collectors.toMap(RoleCapacity::getRole, Function.identity()));
        assertThat(capacities).containsOnlyKeys("Designer", "Marketer", "Pm");
        assertThat(capacities.get("Designer").getCount()).isEqualTo(2);
        assertThat(capacities.get("Designer").getMetricLabel()).isEqualTo("最终报价接受率");
        assertThat(capacities.get("Designer").getMetricValue()).isEqualByComparingTo("66.7");
        assertThat(capacities.get("Marketer").getCount()).isEqualTo(1);
        assertThat(capacities.get("Marketer").getMetricValue()).isEqualByComparingTo("40.0");
        assertThat(capacities.get("Pm").getCount()).isEqualTo(1);
        assertThat(capacities.get("Pm").getMetricValue()).isEqualByComparingTo("80.0");
        assertThat(report.getSampleSufficient()).isFalse();
    }

    @Test
    void shouldAnnotateSingleStoreScopeWithStoreName() {
        com.th4.edge.tenant.user.model.Store store = new com.th4.edge.tenant.user.model.Store();
        store.setStoreId(99L);
        store.setName("城南店");
        when(storeRepository.findByStoreId(99L)).thenReturn(store);
        when(aggregate.organizationCounts(eq(COM_ID), eq(99L), eq(MONTH)))
                .thenReturn(new StatisticStoreAggregateContext.OrganizationCounts(0, 0, Map.of()));
        when(aggregate.organizationSignedAmount(eq(COM_ID), eq(99L), eq(MONTH))).thenReturn(BigDecimal.ZERO);
        when(aggregate.organizationNewProjectCount(eq(COM_ID), eq(99L), eq(MONTH))).thenReturn(0L);
        when(performanceContext.report(any(), any())).thenReturn(performance());

        OrganizationQuery query = query(MONTH, null);
        query.setStoreId(99L);
        OrganizationReport report = context.report(boss, query);

        assertThat(report.getStoreScope().getScopeType()).isEqualTo("Store");
        assertThat(report.getStoreScope().getStoreId()).isEqualTo(99L);
        assertThat(report.getStoreScope().getStoreName()).isEqualTo("城南店");
        assertThat(report.getStoreScope().getUnassignedCount()).isZero();
    }

    @Test
    void shouldRankByRoleSpecificMetricAndLimitTopN() {
        when(aggregate.organizationCounts(anyLong(), anyLong(), any()))
                .thenReturn(new StatisticStoreAggregateContext.OrganizationCounts(2, 0, Map.of(EmployeeRole.Designer, 2L)));
        when(aggregate.organizationSignedAmount(anyLong(), anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(aggregate.organizationNewProjectCount(anyLong(), anyLong(), any())).thenReturn(0L);
        when(performanceContext.report(any(), any())).thenReturn(performance());

        OrganizationReport report = context.report(boss, query(MONTH, 2));

        assertThat(report.getDesignerRanking()).extracting(item -> item.getEmployee().getName())
                .containsExactly("李设计", "王设计");
        assertThat(report.getMarketerRanking()).extracting(item -> item.getEmployee().getName())
                .containsExactly("王芳", "赵敏");
        assertThat(report.getPmRanking()).extracting(item -> item.getEmployee().getName())
                .containsExactly("张工", "李工");
        assertThat(report.getDesignerRanking()).hasSize(2);
    }

    // ------------------------------------------------------------------ 构造

    private OrganizationQuery query(LocalDate month, Integer topN) {
        OrganizationQuery query = new OrganizationQuery();
        query.setMonth(month);
        query.setTopN(topN);
        return query;
    }

    private Employee employee(Long id, String name, EmployeeRole role, EmployeeStatus status, LocalDate joinedAt) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setName(name);
        employee.setRole(role);
        employee.setStatus(status);
        employee.setJoinedAt(joinedAt.atStartOfDay());
        return employee;
    }

    private EmployeePerformance performance(Long id, String name, EmployeeRole role,
                                            BigDecimal designerRate, BigDecimal marketerRate, BigDecimal pmRate) {
        EmployeePerformance item = new EmployeePerformance();
        item.setEmployee(employee(id, name, role, EmployeeStatus.Active, MONTH.minusMonths(1)));
        item.setRole(role);
        item.setDesignerConversionRate(designerRate);
        item.setMarketerSignRate(marketerRate);
        item.setPmOnTimeDeliveryRate(pmRate);
        return item;
    }

    private PerformanceReport performance() {
        PerformanceReport report = new PerformanceReport();
        report.setMonth(MONTH);
        report.setDesignerCount(2);
        report.setMarketerCount(2);
        report.setPmCount(2);
        report.setAverageDesignerConversionRate(new BigDecimal("66.7"));
        report.setAverageMarketerSignRate(new BigDecimal("40.0"));
        report.setAveragePmOnTimeDeliveryRate(new BigDecimal("80.0"));
        report.setPerformances(List.of(
                performance(2L, "李设计", EmployeeRole.Designer, new BigDecimal("80.0"), null, null),
                performance(3L, "王设计", EmployeeRole.Designer, new BigDecimal("53.4"), null, null),
                performance(4L, "王芳", EmployeeRole.Marketer, null, new BigDecimal("45.0"), null),
                performance(8L, "赵敏", EmployeeRole.Marketer, null, new BigDecimal("35.0"), null),
                performance(5L, "张工", EmployeeRole.Pm, null, null, new BigDecimal("92.0")),
                performance(9L, "李工", EmployeeRole.Pm, null, null, new BigDecimal("68.0"))));
        return report;
    }
}
