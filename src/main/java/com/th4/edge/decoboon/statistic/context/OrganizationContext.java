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
    private final EmployeeRepository employeeRepository;
    private final ProjectContractRepository contractRepository;
    private final PerformanceReportContext performanceReportContext;

    public OrganizationContext(TenantContext tenantContext,
                               EmployeeRepository employeeRepository,
                               ProjectContractRepository contractRepository,
                               PerformanceReportContext performanceReportContext) {
        this.tenantContext = tenantContext;
        this.employeeRepository = employeeRepository;
        this.contractRepository = contractRepository;
        this.performanceReportContext = performanceReportContext;
    }

    public OrganizationReport report(Employee principal, OrganizationQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        Long comId = current.getCompany().getComId();
        LocalDate month = query == null || query.getMonth() == null
                ? LocalDate.now().withDayOfMonth(1)
                : query.getMonth().withDayOfMonth(1);
        Long storeId = query == null ? null : query.getStoreId();
        int topN = topN(query);

        List<Employee> employees = employeeRepository.findByCompany_ComId(comId).stream()
                .filter(item -> matchesStore(item, storeId))
                .toList();
        List<Employee> active = employees.stream()
                .filter(item -> item.getStatus() == EmployeeStatus.Active)
                .toList();
        long newHires = active.stream()
                .filter(item -> sameMonth(item.getJoinedAt(), month))
                .count();

        List<ProjectContract> contracts = contractRepository.findByCompany_ComIdOrderByContractIdDesc(comId)
                .stream()
                .filter(item -> matchesStore(item.getProject(), storeId))
                .toList();
        BigDecimal signedAmount = contracts.stream()
                .filter(item -> sameMonth(item.getSignedAt(), month))
                .map(this::contractRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long newProjectCount = contracts.stream()
                .filter(item -> sameMonth(item.getSignedAt(), month))
                .map(item -> item.getProject() == null ? null : item.getProject().getProjectId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        OrganizationReport report = new OrganizationReport();
        report.setMonth(month);
        report.setHeadcount(active.size());
        report.setNewHires((int) newHires);
        report.setSignedAmount(signedAmount);
        report.setNewProjectCount((int) newProjectCount);
        report.setPerCapitaOutput(perCapita(signedAmount, active.size()));
        report.setPerCapitaOrders(perCapita(BigDecimal.valueOf(newProjectCount), active.size()));

        PerformanceReport performance = performanceReportContext.report(principal,
                performanceQuery(month, storeId, null));
        report.setRoleCapacities(roleCapacities(active, performance));
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

    private List<RoleCapacity> roleCapacities(List<Employee> active, PerformanceReport performance) {
        return List.of(
                new RoleCapacity(EmployeeRole.Designer.name(), EmployeeRole.Designer.getName(),
                        countRole(active, EmployeeRole.Designer),
                        "designerConversionRate", "最终报价接受率",
                        average(performance == null ? null : performance.getAverageDesignerConversionRate())),
                new RoleCapacity(EmployeeRole.Marketer.name(), EmployeeRole.Marketer.getName(),
                        countRole(active, EmployeeRole.Marketer),
                        "marketerSignRate", "签约率",
                        average(performance == null ? null : performance.getAverageMarketerSignRate())),
                new RoleCapacity(EmployeeRole.Pm.name(), EmployeeRole.Pm.getName(),
                        countRole(active, EmployeeRole.Pm),
                        "pmOnTimeDeliveryRate", "准时交付率",
                        average(performance == null ? null : performance.getAveragePmOnTimeDeliveryRate()))
        );
    }

    private int countRole(List<Employee> active, EmployeeRole role) {
        return (int) active.stream().filter(item -> item.getRole() == role).count();
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

    private boolean matchesStore(Employee employee, Long storeId) {
        if (storeId == null) {
            return true;
        }
        return employee.getStore() != null && storeId.equals(employee.getStore().getStoreId());
    }

    private boolean matchesStore(com.th4.edge.decoboon.project.model.Project project, Long storeId) {
        if (storeId == null) {
            return true;
        }
        return project != null && project.getStore() != null && storeId.equals(project.getStore().getStoreId());
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
