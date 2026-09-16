package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinanceProfitAccountingTest {

    @Test
    void shouldCalculateGrossProfitByCostCategory() {
        FinanceAccessContext access = mock(FinanceAccessContext.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
        PayableRepository payableRepository = mock(PayableRepository.class);
        LedgerRepository ledgerRepository = mock(LedgerRepository.class);
        SettlementRepository settlementRepository = mock(SettlementRepository.class);
        FinanceReportContext context = new FinanceReportContext(access, projectRepository,
                receivableRepository, payableRepository, ledgerRepository, settlementRepository);

        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setEmpId(2L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        Project project = new Project();
        project.setProjectId(3L);
        project.setCompany(company);
        project.setContractAmount(new BigDecimal("10000.00"));

        List<Payable> payables = List.of(
                payable(project, PayableType.Material, "3000.00"),
                payable(project, PayableType.Salary, "2000.00"),
                payable(project, PayableType.Project, "500.00"),
                payable(project, PayableType.Expense, "1000.00"));

        when(access.requireReader(boss)).thenReturn(boss);
        when(access.resolveProject(boss, 3L)).thenReturn(project);
        when(access.value(any())).thenAnswer(invocation -> invocation.getArgument(0) == null
                ? BigDecimal.ZERO : invocation.getArgument(0));
        when(access.outstanding(any(), any())).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(0) == null ? BigDecimal.ZERO : invocation.getArgument(0);
            BigDecimal paid = invocation.getArgument(1) == null ? BigDecimal.ZERO : invocation.getArgument(1);
            return amount.subtract(paid);
        });
        when(receivableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(List.of());
        when(payableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(payables);
        when(ledgerRepository.findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(1L, 3L))
                .thenReturn(List.of());
        when(settlementRepository.findByProject_ProjectIdAndCompany_ComId(3L, 1L)).thenReturn(java.util.Optional.empty());

        FinanceProjectReport report = context.project(boss, 3L);

        assertThat(report.getContractRevenue()).isEqualByComparingTo("10000.00");
        assertThat(report.getMaterialCost()).isEqualByComparingTo("3000.00");
        assertThat(report.getLaborCost()).isEqualByComparingTo("2000.00");
        assertThat(report.getProjectCost()).isEqualByComparingTo("500.00");
        assertThat(report.getExpenseAllocation()).isEqualByComparingTo("1000.00");
        assertThat(report.getTotalCost()).isEqualByComparingTo("6500.00");
        assertThat(report.getGrossProfit()).isEqualByComparingTo("3500.00");
        assertThat(report.getGrossMargin()).isEqualByComparingTo("35.0");
    }

    private Payable payable(Project project, PayableType type, String amount) {
        Payable payable = new Payable();
        payable.setProject(project);
        payable.setType(type);
        payable.setAmount(new BigDecimal(amount));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setDueDate(java.time.LocalDate.now());
        return payable;
    }
}
