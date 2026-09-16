package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.statistic.context.BossDashboardContext;
import com.th4.edge.decoboon.statistic.model.BossDashboard;
import com.th4.edge.decoboon.statistic.model.BossDashboardQuery;
import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.decoboon.statistic.repository.BusinessTargetRepository;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BossDashboardContextTest {

    @Test
    void shouldBuildFourMetricsWithTargetAndComparisons() {
        TenantContext tenant = mock(TenantContext.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectContractRepository contracts = mock(ProjectContractRepository.class);
        ReceiptRepository receipts = mock(ReceiptRepository.class);
        PayableRepository payables = mock(PayableRepository.class);
        BusinessTargetRepository targets = mock(BusinessTargetRepository.class);
        ProjectCostContext costs = mock(ProjectCostContext.class);
        BossDashboardContext context = new BossDashboardContext(tenant, projects, contracts, receipts, payables, targets, costs);

        LocalDate month = LocalDate.of(2026, 9, 1);
        LocalDate previousMonth = month.minusMonths(1);
        Company company = new Company();
        company.setComId(10L);
        Employee boss = new Employee();
        boss.setEmpId(11L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        Project currentProject = project(20L, company, month);
        Project previousProject = project(21L, company, previousMonth);

        ProjectContract currentContract = contract(30L, currentProject, month, "10000.00");
        ProjectContract previousContract = contract(31L, previousProject, previousMonth, "5000.00");
        Receipt currentReceipt = receipt(currentProject, month, "3000.00");
        Receipt previousReceipt = receipt(previousProject, previousMonth, "1500.00");
        Payable currentCost = payable(currentProject, "4000.00");
        Payable previousCost = payable(previousProject, "1000.00");
        BusinessTarget target = target(company, month);

        when(tenant.requireBoss(boss)).thenReturn(boss);
        when(projects.findByCompany_ComIdOrderByProjectIdDesc(10L)).thenReturn(List.of(currentProject, previousProject));
        when(contracts.findByCompany_ComIdOrderByContractIdDesc(10L)).thenReturn(List.of(currentContract, previousContract));
        when(receipts.findByCompany_ComIdOrderByReceivedAtDesc(10L)).thenReturn(List.of(currentReceipt, previousReceipt));
        when(payables.findByCompany_ComIdOrderByDueDateAsc(10L)).thenReturn(List.of(currentCost, previousCost));
        when(targets.findByCompany_ComIdAndTargetMonth(10L, month)).thenReturn(Optional.of(target));
        when(costs.summarize(currentProject)).thenReturn(
                new ProjectCostContext.CostSummary(new BigDecimal("4000.00"), new BigDecimal("4000.00"), 0));
        when(costs.summarize(previousProject)).thenReturn(
                new ProjectCostContext.CostSummary(new BigDecimal("1000.00"), new BigDecimal("1000.00"), 0));

        BossDashboardQuery query = new BossDashboardQuery();
        query.setMonth(month);
        BossDashboard dashboard = context.dashboard(boss, query);

        assertThat(dashboard.getSignedAmount().getValue()).isEqualByComparingTo("10000.00");
        assertThat(dashboard.getSignedAmount().getTarget()).isEqualByComparingTo("12000.00");
        assertThat(dashboard.getSignedAmount().getAchievementRate()).isEqualByComparingTo("83.3");
        assertThat(dashboard.getSignedAmount().getMonthOverMonthRate()).isEqualByComparingTo("100.0");
        assertThat(dashboard.getReceivedAmount().getValue()).isEqualByComparingTo("3000.00");
        assertThat(dashboard.getGrossProfit().getValue()).isEqualByComparingTo("6000.00");
        assertThat(dashboard.getActiveProjectCount().getValue()).isEqualByComparingTo("2");
    }

    private Project project(Long id, Company company, LocalDate date) {
        Project project = new Project();
        project.setProjectId(id);
        project.setCompany(company);
        project.setCreateTime(new DateTime(date.atStartOfDay()));
        return project;
    }

    private ProjectContract contract(Long id, Project project, LocalDate signedAt, String amount) {
        ProjectContract contract = new ProjectContract();
        contract.setContractId(id);
        contract.setProject(project);
        contract.setSignedAt(signedAt);
        contract.setBaselineAmount(new BigDecimal(amount));
        contract.setCurrentAmount(new BigDecimal(amount));
        return contract;
    }

    private Receipt receipt(Project project, LocalDate date, String amount) {
        Receipt receipt = new Receipt();
        receipt.setProject(project);
        receipt.setReceivedAt(date);
        receipt.setAmount(new BigDecimal(amount));
        return receipt;
    }

    private Payable payable(Project project, String amount) {
        Payable payable = new Payable();
        payable.setProject(project);
        payable.setAmount(new BigDecimal(amount));
        payable.setPaidAmount(BigDecimal.ZERO);
        return payable;
    }

    private BusinessTarget target(Company company, LocalDate month) {
        BusinessTarget target = new BusinessTarget();
        target.setCompany(company);
        target.setTargetMonth(month);
        target.setSignedAmount(new BigDecimal("12000.00"));
        target.setReceivedAmount(new BigDecimal("5000.00"));
        target.setGrossProfit(new BigDecimal("7000.00"));
        target.setActiveProjectCount(3);
        return target;
    }
}
