package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.context.QuoteAcceptanceContext;
import com.th4.edge.decoboon.statistic.model.EmployeePerformance;
import com.th4.edge.decoboon.statistic.model.PerformanceQuery;
import com.th4.edge.decoboon.statistic.model.PerformanceReport;
import com.th4.edge.decoboon.statistic.model.StoreScopeInfo;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.th4.edge.decoboon.common.context.StoreScope;

/** 人员绩效自动考核计算。 */
@Component
public class PerformanceReportContext {

    private static final Set<EmployeeRole> PERFORMANCE_ROLES = Set.of(
            EmployeeRole.Designer, EmployeeRole.Marketer, EmployeeRole.Pm);
    private static final Set<CustStatus> SIGNED_STATUSES = Set.of(
            CustStatus.Signed, CustStatus.Serving, CustStatus.Finished);

    private final TenantContext tenantContext;
    private final EmployeeRepository employeeRepository;
    private final QuoteRepository quoteRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final AcceptanceRepository acceptanceRepository;

    /** 门店维度范围标注与未归属员工计数（本批新增只读补充，不改绩效口径）。 */
    @org.springframework.beans.factory.annotation.Autowired
    private StatisticStoreAggregateContext aggregateContext;

    @org.springframework.beans.factory.annotation.Autowired
    private StoreRepository storeRepository;

    public PerformanceReportContext(TenantContext tenantContext,
                                    EmployeeRepository employeeRepository,
                                    QuoteRepository quoteRepository,
                                    CustomerRepository customerRepository,
                                    ProjectRepository projectRepository,
                                    AcceptanceRepository acceptanceRepository) {
        this.tenantContext = tenantContext;
        this.employeeRepository = employeeRepository;
        this.quoteRepository = quoteRepository;
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.acceptanceRepository = acceptanceRepository;
    }

    public PerformanceReport report(Employee principal, PerformanceQuery query) {
        Employee current = tenantContext.requireRole(principal,
                EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.TeamLeader);
        if (current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        LocalDate month = query == null || query.getMonth() == null
                ? LocalDate.now().withDayOfMonth(1)
                : query.getMonth().withDayOfMonth(1);
        EmployeeRole requestedRole = query == null ? null : query.getRole();
        if (requestedRole != null && !PERFORMANCE_ROLES.contains(requestedRole)) {
            throw new DefinedException(new DefaultErrorCode("400", "仅支持设计师、顾问和项目经理绩效"));
        }

        Long comId = current.getCompany().getComId();
        // 门店维度（DEC-067）：不传 / null / 0 等价为全部门店
        Long storeId = StoreScope.normalize(query == null ? null : query.getStoreId());
        // P0B-02 聚合下推：员工、报价、客户、项目、验收的门店条件全部在 SQL 侧完成
        // （`DEC-067` 门店维度；全部门店时 SQL 中不出现门店条件，含未归属记录），
        // 人员绩效的比率公式仍在内存计算（`BR-031` 业务公式例外，循环体内零查询）。
        boolean allStores = StoreScope.isAllStores(storeId);
        Long scopedStoreId = allStores ? 0L : storeId;
        List<Employee> employees = employees(current, scopedStoreId, allStores).stream()
                .filter(employee -> PERFORMANCE_ROLES.contains(employee.getRole()))
                .filter(employee -> requestedRole == null || employee.getRole() == requestedRole)
                .toList();
        final List<Quote> quotes = quoteRepository.findOpportunityQuotesByStoreScope(comId,
                com.th4.edge.decoboon.common.configure.BusinessOrigin.New,
                QuoteStatus.Draft, QuoteStatus.Superseded, allStores, scopedStoreId);
        final List<Customer> customers = customerRepository.findByCompanyStoreScope(comId, allStores, scopedStoreId);
        final List<Project> projects = projectRepository.findByCompanyStoreScope(comId, allStores, scopedStoreId);
        List<Long> scopedProjectIds = projects.stream().map(Project::getProjectId).toList();
        List<Acceptance> acceptances = scopedProjectIds.isEmpty()
                ? List.of()
                : acceptanceRepository.findByCompanyAndProjectIds(comId, scopedProjectIds);

        List<EmployeePerformance> performances = employees.stream()
                .map(employee -> performance(employee, quotes, customers, projects, acceptances, month))
                .toList();
        PerformanceReport report = new PerformanceReport();
        report.setMonth(month);
        report.setDesignerCount(count(performances, EmployeeRole.Designer));
        report.setMarketerCount(count(performances, EmployeeRole.Marketer));
        report.setPmCount(count(performances, EmployeeRole.Pm));
        report.setAverageDesignerConversionRate(average(performances, EmployeeRole.Designer,
                EmployeePerformance::getDesignerConversionRate));
        report.setAverageMarketerSignRate(average(performances, EmployeeRole.Marketer,
                EmployeePerformance::getMarketerSignRate));
        report.setAveragePmOnTimeDeliveryRate(average(performances, EmployeeRole.Pm,
                EmployeePerformance::getPmOnTimeDeliveryRate));
        report.setAveragePmAcceptancePassRate(average(performances, EmployeeRole.Pm,
                EmployeePerformance::getPmAcceptancePassRate));
        report.setPerformances(performances);
        report.setStoreScope(storeScopeInfo(comId, storeId, allStores));
        return report;
    }

    private EmployeePerformance performance(Employee employee,
                                            List<Quote> quotes,
                                            List<Customer> customers,
                                            List<Project> projects,
                                            List<Acceptance> acceptances,
                                            LocalDate month) {
        EmployeePerformance performance = new EmployeePerformance();
        performance.setEmployee(employee);
        performance.setRole(employee.getRole());
        if (employee.getRole() == EmployeeRole.Designer) {
            List<Quote> employeeQuotes = quotes.stream()
                    .filter(quote -> sameEmployee(quote.getCreatedBy(), employee))
                    .filter(quote -> sameMonth(quote.getCreateTime() == null ? null : quote.getCreateTime().toLocalDate(), month))
                    .toList();
            // 口径统一（MET-001 / DEC-001）：机会按客户 + 房屋聚合，最终决策按 decidedAt 归属统计周期。
            List<QuoteAcceptanceContext.Opportunity> monthOpportunities =
                    QuoteAcceptanceContext.opportunities(quotes).stream()
                            .filter(opportunity -> opportunity.ownedBy(employee))
                            .filter(opportunity -> sameMonth(
                                    opportunity.decidedAt() == null ? null : opportunity.decidedAt().toLocalDate(), month))
                            .toList();
            QuoteAcceptanceContext.Result acceptance =
                    QuoteAcceptanceContext.summarize(monthOpportunities, employeeQuotes);
            performance.setQuoteCount(acceptance.submittedQuoteCount());
            performance.setAcceptedQuoteCount(acceptance.acceptedOpportunityCount());
            performance.setPendingDecisionQuoteCount(acceptance.pendingDecisionQuoteCount());
            performance.setDecidedOpportunityCount(acceptance.decidedOpportunityCount());
            performance.setFirstQuoteAcceptanceRate(acceptance.firstAcceptanceRate());
            performance.setDesignerConversionRate(acceptance.finalAcceptanceRate());
        } else if (employee.getRole() == EmployeeRole.Marketer) {
            List<Customer> employeeCustomers = customers.stream()
                    .filter(customer -> sameEmployee(customer.getOwner(), employee))
                    .filter(customer -> sameMonth(customer.getCreateTime() == null ? null : customer.getCreateTime().toLocalDate(), month))
                    .toList();
            int signed = (int) employeeCustomers.stream()
                    .filter(customer -> SIGNED_STATUSES.contains(customer.getStatus()))
                    .count();
            performance.setCustomerCount(employeeCustomers.size());
            performance.setSignedCustomerCount(signed);
            performance.setMarketerSignRate(rate(signed, employeeCustomers.size()));
        } else if (employee.getRole() == EmployeeRole.Pm) {
            List<Project> managed = projects.stream()
                    .filter(project -> sameEmployee(project.getManager(), employee))
                    .toList();
            List<Project> delivered = managed.stream()
                    .filter(project -> project.getStatus() == ProjectStatus.Finished
                            || project.getStatus() == ProjectStatus.Settled)
                    .filter(project -> project.getActualEndDate() != null
                            && YearMonth.from(project.getActualEndDate()).equals(YearMonth.from(month)))
                    .toList();
            int onTime = (int) delivered.stream()
                    .filter(project -> project.getPlanEndDate() != null
                            && !project.getActualEndDate().isAfter(project.getPlanEndDate()))
                    .count();
            performance.setDeliveredProjectCount(delivered.size());
            performance.setOnTimeProjectCount(onTime);
            performance.setPmOnTimeDeliveryRate(rate(onTime, delivered.size()));

            Set<Long> projectIds = managed.stream().map(Project::getProjectId).collect(java.util.stream.Collectors.toSet());
            List<Acceptance> projectAcceptances = acceptances.stream()
                    .filter(acceptance -> acceptance.getProject() != null
                            && projectIds.contains(acceptance.getProject().getProjectId()))
                    .filter(acceptance -> acceptance.getCreateTime() != null
                            && YearMonth.from(acceptance.getCreateTime().toLocalDate()).equals(YearMonth.from(month)))
                    .filter(acceptance -> acceptance.getStatus() == AcceptStatus.Confirmed
                            || acceptance.getStatus() == AcceptStatus.Rejected)
                    .toList();
            int passed = (int) projectAcceptances.stream()
                    .filter(acceptance -> acceptance.getStatus() == AcceptStatus.Confirmed)
                    .count();
            performance.setAcceptanceCount(projectAcceptances.size());
            performance.setAcceptedAcceptanceCount(passed);
            performance.setPmAcceptancePassRate(rate(passed, projectAcceptances.size()));
        }
        return performance;
    }

    /**
     * 在范围内的员工：BOSS 按企业 + 门店维度（SQL 门店条件）；店长锁定本店；团队负责人按团队
     * （门店维度为单店时一并下推 SQL，全部门店时按团队）。
     */
    private List<Employee> employees(Employee current, Long storeId, boolean allStores) {
        Long comId = current.getCompany().getComId();
        if (current.getRole() == EmployeeRole.Boss) {
            return employeeRepository.findByCompanyStoreScope(comId, allStores, storeId);
        }
        if (current.getRole() == EmployeeRole.StoreManager && current.getStore() != null) {
            return employeeRepository.findByCompany_ComIdAndStore_StoreIdOrderByName(
                    comId, current.getStore().getStoreId());
        }
        if (current.getRole() == EmployeeRole.TeamLeader && current.getTeam() != null) {
            if (allStores) {
                return employeeRepository.findByCompany_ComIdAndTeam_TeamIdOrderByName(
                        comId, current.getTeam().getTeamId());
            }
            return employeeRepository.findByCompany_ComIdAndTeam_TeamIdAndStore_StoreIdOrderByName(
                    comId, current.getTeam().getTeamId(), storeId);
        }
        return List.of();
    }

    private int count(List<EmployeePerformance> performances, EmployeeRole role) {
        return (int) performances.stream().filter(item -> item.getRole() == role).count();
    }

    private BigDecimal average(List<EmployeePerformance> performances,
                               EmployeeRole role,
                               java.util.function.Function<EmployeePerformance, BigDecimal> getter) {
        List<BigDecimal> values = performances.stream()
                .filter(item -> item.getRole() == role)
                .map(getter)
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    private boolean sameEmployee(Employee left, Employee right) {
        return left != null && right != null && left.getEmpId() != null
                && left.getEmpId().equals(right.getEmpId());
    }

    private boolean sameMonth(LocalDate date, LocalDate month) {
        return date != null && YearMonth.from(date).equals(YearMonth.from(month));
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    /** 门店维度范围标注（`DEC-067` BOSS-STORE-03）：未归属员工数由 SQL `COUNT` 提供，单店视图恒 0。 */
    private StoreScopeInfo storeScopeInfo(Long comId, Long storeId, boolean allStores) {
        StoreScopeInfo info = new StoreScopeInfo();
        info.setScopeType(allStores ? "AllStores" : "Store");
        if (!allStores) {
            info.setStoreId(storeId);
            Store store = storeRepository.findByStoreId(storeId);
            if (store != null) {
                info.setStoreName(store.getName());
            }
            return info;
        }
        info.setUnassignedCount((int) aggregateContext.unassignedEmployees(comId));
        return info;
    }
}
