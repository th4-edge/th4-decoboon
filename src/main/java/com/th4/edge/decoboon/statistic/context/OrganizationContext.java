package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.statistic.model.EmployeePerformance;
import com.th4.edge.decoboon.statistic.model.OrganizationQuery;
import com.th4.edge.decoboon.statistic.model.OrganizationReport;
import com.th4.edge.decoboon.statistic.model.PerformanceQuery;
import com.th4.edge.decoboon.statistic.model.PerformanceReport;
import com.th4.edge.decoboon.statistic.model.RoleCapacity;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.statistic.model.StoreScopeInfo;

/**
 * 企业组织效能聚合。
 *
 * <p>人员规模与人均口径在本上下文计算；角色绩效均值与排行复用
 * {@link PerformanceReportContext}，不另起一套考核规则（与 BusinessAlertType/nextAction 的同源原则一致）。</p>
 */
@Component
public class OrganizationContext {

    private static final int DEFAULT_TOP_N = 5;
    private static final int MAX_TOP_N = 20;
    private static final int MIN_SAMPLE = 3;

    private final TenantContext tenantContext;
    private final com.th4.edge.tenant.user.repository.StoreRepository storeRepository;
    private final PerformanceReportContext performanceReportContext;
    private final StatisticStoreAggregateContext aggregateContext;

    public OrganizationContext(TenantContext tenantContext,
                               com.th4.edge.tenant.user.repository.StoreRepository storeRepository,
                               PerformanceReportContext performanceReportContext,
                               StatisticStoreAggregateContext aggregateContext) {
        this.tenantContext = tenantContext;
        this.storeRepository = storeRepository;
        this.performanceReportContext = performanceReportContext;
        this.aggregateContext = aggregateContext;
    }

    public OrganizationReport report(Employee principal, OrganizationQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        LocalDate month = query == null || query.getMonth() == null
                ? LocalDate.now().withDayOfMonth(1)
                : query.getMonth().withDayOfMonth(1);
        Long storeId = StoreScope.normalize(query == null ? null : query.getStoreId());
        int topN = topN(query);

        // P0B-02 聚合下推（DEC-036 / BR-031）：人力计数、本月入职、角色在编、签约额与新项目数
        // 全部在 SQL 侧完成；门店条件（null/0/不传 = 全部门店）同样在 SQL 的 WHERE 里。
        StatisticStoreAggregateContext.OrganizationCounts counts =
                aggregateContext.organizationCounts(comId, storeId, month);
        BigDecimal signedAmount = aggregateContext.organizationSignedAmount(comId, storeId, month);
        long newProjectCount = aggregateContext.organizationNewProjectCount(comId, storeId, month);

        OrganizationReport report = new OrganizationReport();
        report.setMonth(month);
        report.setStoreScope(storeScopeInfo(comId, storeId));
        report.setHeadcount(counts.headcount());
        report.setNewHires(counts.newHires());
        report.setSignedAmount(signedAmount);
        report.setNewProjectCount((int) newProjectCount);
        report.setPerCapitaOutput(perCapita(signedAmount, counts.headcount()));
        report.setPerCapitaOrders(perCapita(BigDecimal.valueOf(newProjectCount), counts.headcount()));

        PerformanceReport performance = performanceReportContext.report(principal,
                performanceQuery(month, storeId, null));
        report.setRoleCapacities(roleCapacities(counts.roleCounts(), performance));
        report.setDesignerRanking(ranking(performance, EmployeeRole.Designer, topN));
        report.setMarketerRanking(ranking(performance, EmployeeRole.Marketer, topN));
        report.setPmRanking(ranking(performance, EmployeeRole.Pm, topN));
        report.setSampleSufficient(performance == null
                || (performance.getDesignerCount() >= MIN_SAMPLE
                && performance.getMarketerCount() >= MIN_SAMPLE
                && performance.getPmCount() >= MIN_SAMPLE));
        return report;
    }

    // ------------------------------------------------------------------ 角色产能

    private List<RoleCapacity> roleCapacities(java.util.Map<Object, Long> roleCounts, PerformanceReport performance) {
        return List.of(
                new RoleCapacity(EmployeeRole.Designer.name(), EmployeeRole.Designer.getName(),
                        roleCount(roleCounts, EmployeeRole.Designer),
                        "designerConversionRate", "最终报价接受率",
                        average(performance == null ? null : performance.getAverageDesignerConversionRate())),
                new RoleCapacity(EmployeeRole.Marketer.name(), EmployeeRole.Marketer.getName(),
                        roleCount(roleCounts, EmployeeRole.Marketer),
                        "marketerSignRate", "签约率",
                        average(performance == null ? null : performance.getAverageMarketerSignRate())),
                new RoleCapacity(EmployeeRole.Pm.name(), EmployeeRole.Pm.getName(),
                        roleCount(roleCounts, EmployeeRole.Pm),
                        "pmOnTimeDeliveryRate", "准时交付率",
                        average(performance == null ? null : performance.getAveragePmOnTimeDeliveryRate()))
        );
    }

    /** 角色在编人数（SQL `GROUP BY role` 结果）。 */
    private int roleCount(java.util.Map<Object, Long> roleCounts, EmployeeRole role) {
        Long value = roleCounts.get(role);
        return value == null ? 0 : value.intValue();
    }

    // ------------------------------------------------------------------ 排行

    private List<EmployeePerformance> ranking(PerformanceReport performance, EmployeeRole role, int topN) {
        if (performance == null || performance.getPerformances() == null) {
            return List.of();
        }
        return performance.getPerformances().stream()
                .filter(item -> item.getRole() == role)
                .filter(item -> item.getEmployee() != null && item.getEmployee().getStatus() == EmployeeStatus.Active)
                .sorted(comparator(role))
                .limit(topN)
                .toList();
    }

    private Comparator<EmployeePerformance> comparator(EmployeeRole role) {
        return switch (role) {
            case Designer -> Comparator.comparing(EmployeePerformance::getDesignerConversionRate,
                    Comparator.nullsFirst(Comparator.reverseOrder()));
            case Marketer -> Comparator.comparing(EmployeePerformance::getMarketerSignRate,
                    Comparator.nullsFirst(Comparator.reverseOrder()));
            default -> Comparator.comparing(EmployeePerformance::getPmOnTimeDeliveryRate,
                    Comparator.nullsFirst(Comparator.reverseOrder()));
        };
    }

    // ------------------------------------------------------------------ 工具


    /**
     * 门店维度范围标注（`DEC-067` BOSS-STORE-03）：全部门店计入「未归属门店」记录并显式标注；
     * 单店视图不含未归属 → `unassignedCount = 0`。`storeName` 取自当前范围内任一带门店的记录。
     */
    private StoreScopeInfo storeScopeInfo(Long comId, Long storeId) {
        StoreScopeInfo info = new StoreScopeInfo();
        boolean allStores = StoreScope.isAllStores(storeId);
        info.setScopeType(allStores ? "AllStores" : "Store");
        if (!allStores) {
            info.setStoreId(storeId);
            com.th4.edge.tenant.user.model.Store store = storeRepository.findByStoreId(storeId);
            if (store != null) {
                info.setStoreName(store.getName());
            }
            return info;
        }
        info.setUnassignedCount((int) aggregateContext.unassignedEmployees(comId));
        return info;
    }

    private PerformanceQuery performanceQuery(LocalDate month, Long storeId, EmployeeRole role) {
        PerformanceQuery query = new PerformanceQuery();
        query.setMonth(month);
        query.setStoreId(storeId);
        query.setRole(role);
        return query;
    }

    private int topN(OrganizationQuery query) {
        if (query == null || query.getTopN() == null || query.getTopN() <= 0) {
            return DEFAULT_TOP_N;
        }
        return Math.min(query.getTopN(), MAX_TOP_N);
    }

    private BigDecimal perCapita(BigDecimal numerator, int headcount) {
        if (headcount <= 0) {
            return BigDecimal.ZERO;
        }
        return value(numerator).divide(BigDecimal.valueOf(headcount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal contractRevenue(ProjectContract contract) {
        return value(contract.getCurrentAmount()).signum() == 0
                ? value(contract.getBaselineAmount()) : value(contract.getCurrentAmount());
    }



    private boolean sameMonth(java.time.LocalDateTime date, LocalDate month) {
        return date != null && sameMonth(date.toLocalDate(), month);
    }

    private boolean sameMonth(LocalDate date, LocalDate month) {
        return date != null && YearMonth.from(date).equals(YearMonth.from(month));
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
