package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.statistic.context.OrganizationContext;
import com.th4.edge.decoboon.statistic.context.PerformanceReportContext;
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
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 组织效能聚合：
 * 在职/新增口径、人均产值与人均单产、角色产能（角色 + 人数 + 该角色绩效均值）、三张排行与样本量提示。
 */
class OrganizationContextTest {

    private static final long COM_ID = 21L;
    private static final LocalDate MONTH = LocalDate.of(2026, 9, 1);

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final ProjectContractRepository contractRepository = mock(ProjectContractRepository.class);
    private final PerformanceReportContext performanceContext = mock(PerformanceReportContext.class);

    private final OrganizationContext context = new OrganizationContext(tenantContext, employeeRepository,
            contractRepository, performanceContext);

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
    void shouldAggregateHeadcountPerCapitaAndRoleCapacity() {
        Employee designerA = employee(2L, "李设计", EmployeeRole.Designer, EmployeeStatus.Active, MONTH.plusDays(3));
        Employee designerB = employee(3L, "王设计", EmployeeRole.Designer, EmployeeStatus.Active, MONTH.minusMonths(6));
        Employee marketer = employee(4L, "王芳", EmployeeRole.Marketer, EmployeeStatus.Active, MONTH.minusMonths(8));
        Employee pm = employee(5L, "张工", EmployeeRole.Pm, EmployeeStatus.Active, MONTH.minusMonths(3));
        Employee resigned = employee(6L, "离职设计", EmployeeRole.Designer, EmployeeStatus.Resigned, MONTH.minusMonths(14));
        Employee suspended = employee(7L, "停用业务", EmployeeRole.Marketer, EmployeeStatus.Suspended, MONTH.minusMonths(2));

        when(employeeRepository.findByCompany_ComId(COM_ID))
                .thenReturn(List.of(designerA, designerB, marketer, pm, resigned, suspended));

        Project projectA = project(101L);
        Project projectB = project(102L);
        when(contractRepository.findByCompany_ComIdOrderByContractIdDesc(COM_ID)).thenReturn(List.of(
                contract(projectA, "100000", MONTH.plusDays(5)),
                contract(projectB, "300000", MONTH.plusDays(12)),
                // 上个月签约，不计入本月
                contract(project(103L), "900000", MONTH.minusMonths(1).plusDays(4))));

        when(performanceContext.report(any(), any())).thenReturn(performance());

        OrganizationReport report = context.report(boss, query(MONTH, null));

        assertThat(report.getMonth()).isEqualTo(MONTH);
        // 在职 4 人：离职、停用不计入
        assertThat(report.getHeadcount()).isEqualTo(4);
        // 本月新增 1 人（joinedAt 落在本月）
        assertThat(report.getNewHires()).isEqualTo(1);
        assertThat(report.getSignedAmount()).isEqualByComparingTo("400000");
        assertThat(report.getNewProjectCount()).isEqualTo(2);
        // 人均产值 = 400000 / 4 = 100000
        assertThat(report.getPerCapitaOutput()).isEqualByComparingTo("100000.00");
        // 人均单产 = 2 / 4 = 0.50
        assertThat(report.getPerCapitaOrders()).isEqualByComparingTo("0.50");

        Map<String, RoleCapacity> capacities = report.getRoleCapacities().stream()
                .collect(Collectors.toMap(RoleCapacity::getRole, Function.identity()));
        assertThat(capacities).containsOnlyKeys("Designer", "Marketer", "Pm");
        assertThat(capacities.get("Designer").getCount()).isEqualTo(2);
        assertThat(capacities.get("Designer").getMetricLabel()).isEqualTo("报价接受率");
        assertThat(capacities.get("Designer").getMetricValue()).isEqualByComparingTo("66.7");
        assertThat(capacities.get("Marketer").getCount()).isEqualTo(1);
        assertThat(capacities.get("Marketer").getMetricValue()).isEqualByComparingTo("40.0");
        assertThat(capacities.get("Pm").getCount()).isEqualTo(1);
        assertThat(capacities.get("Pm").getMetricValue()).isEqualByComparingTo("80.0");
        assertThat(report.getSampleSufficient()).isFalse();
    }

    @Test
    void shouldRankByRoleSpecificMetricAndLimitTopN() {
        when(employeeRepository.findByCompany_ComId(COM_ID)).thenReturn(List.of(
                employee(2L, "李设计", EmployeeRole.Designer, EmployeeStatus.Active, MONTH),
                employee(3L, "王设计", EmployeeRole.Designer, EmployeeStatus.Active, MONTH)));

        when(contractRepository.findByCompany_ComIdOrderByContractIdDesc(COM_ID)).thenReturn(List.of());
        when(performanceContext.report(any(), any())).thenReturn(performance());

        OrganizationReport report = context.report(boss, query(MONTH, 2));

        // 设计师按报价接受率倒序
        assertThat(report.getDesignerRanking()).extracting(item -> item.getEmployee().getName())
                .containsExactly("李设计", "王设计");
        // 业务员按签约率倒序
        assertThat(report.getMarketerRanking()).extracting(item -> item.getEmployee().getName())
                .containsExactly("王芳", "赵敏");
        // 项目经理按准时交付率倒序
        assertThat(report.getPmRanking()).extracting(item -> item.getEmployee().getName())
                .containsExactly("张工", "李工");
        // topN 生效
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

    private Project project(Long id) {
        Project project = new Project();
        project.setProjectId(id);
        project.setName("项目 " + id);
        return project;
    }

    private ProjectContract contract(Project project, String amount, LocalDate signedAt) {
        ProjectContract contract = new ProjectContract();
        contract.setProject(project);
        contract.setCurrentAmount(new BigDecimal(amount));
        contract.setBaselineAmount(new BigDecimal(amount));
        contract.setSignedAt(signedAt);
        contract.setStatus(ContractStatus.Effective);
        return contract;
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
