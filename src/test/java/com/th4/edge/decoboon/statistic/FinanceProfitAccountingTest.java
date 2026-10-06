package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceReportContext;
import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
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
        ProjectCostContext projectCostContext = mock(ProjectCostContext.class);
        FinanceReportContext context = new FinanceReportContext(access, projectRepository,
                receivableRepository, payableRepository, ledgerRepository, settlementRepository,
                projectCostContext);

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
        when(projectCostContext.summarize(project)).thenReturn(
                new ProjectCostContext.CostSummary(new BigDecimal("6500.00"), new BigDecimal("6500.00"), 0));

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

    @Test
    void shouldNotReturnReliableProfitWhenCostIsIncomplete() {
        FinanceAccessContext access = mock(FinanceAccessContext.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
        PayableRepository payableRepository = mock(PayableRepository.class);
        LedgerRepository ledgerRepository = mock(LedgerRepository.class);
        SettlementRepository settlementRepository = mock(SettlementRepository.class);
        ProjectCostContext projectCostContext = mock(ProjectCostContext.class);
        FinanceReportContext context = new FinanceReportContext(access, projectRepository,
                receivableRepository, payableRepository, ledgerRepository, settlementRepository,
                projectCostContext);

        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        Project project = new Project();
        project.setProjectId(3L);
        project.setCompany(company);
        project.setContractAmount(new BigDecimal("10000.00"));

        when(access.requireReader(boss)).thenReturn(boss);
        when(access.resolveProject(boss, 3L)).thenReturn(project);
        when(access.value(any())).thenAnswer(invocation -> invocation.getArgument(0) == null
                ? BigDecimal.ZERO : invocation.getArgument(0));
        when(access.outstanding(any(), any())).thenReturn(BigDecimal.ZERO);
        when(receivableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(List.of());
        when(payableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(List.of());
        when(ledgerRepository.findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(1L, 3L))
                .thenReturn(List.of());
        when(projectCostContext.summarize(project)).thenReturn(
                new ProjectCostContext.CostSummary(new BigDecimal("6800.00"), new BigDecimal("5100.00"), 2));

        FinanceProjectReport report = context.project(boss, 3L);

        assertThat(report.getCostComplete()).isFalse();
        assertThat(report.getCostIncompleteCount()).isEqualTo(2);
        assertThat(report.getProfitAmount()).isNull();
        assertThat(report.getGrossProfit()).isNull();
        assertThat(report.getGrossMargin()).isNull();
    }

    @Test
    void summaryMustHideProfitWhenQuoteHasNoItemsButMaterialPayableExists() {
        FinanceAccessContext access = mock(FinanceAccessContext.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
        PayableRepository payableRepository = mock(PayableRepository.class);
        LedgerRepository ledgerRepository = mock(LedgerRepository.class);
        SettlementRepository settlementRepository = mock(SettlementRepository.class);
        ProjectCostContext projectCostContext = mock(ProjectCostContext.class);
        FinanceReportContext context = new FinanceReportContext(access, projectRepository,
                receivableRepository, payableRepository, ledgerRepository, settlementRepository,
                projectCostContext);
        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setEmpId(2L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        Project project = new Project();
        project.setProjectId(3L);
        project.setCompany(company);
        project.setContractAmount(new BigDecimal("50000.00"));
        Payable material = payable(project, PayableType.Material, "1600.00");

        when(access.requireReader(boss)).thenReturn(boss);
        when(access.visible(boss, Project.class)).thenReturn((root, query, cb) -> cb.conjunction());
        when(access.visible(boss, Receivable.class)).thenReturn((root, query, cb) -> cb.conjunction());
        when(access.visible(boss, Payable.class)).thenReturn((root, query, cb) -> cb.conjunction());
        when(access.visible(boss, com.th4.edge.decoboon.finance.model.Ledger.class))
                .thenReturn((root, query, cb) -> cb.conjunction());
        when(access.value(any())).thenAnswer(invocation -> invocation.getArgument(0) == null
                ? BigDecimal.ZERO : invocation.getArgument(0));
        when(access.outstanding(any(), any())).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(0) == null ? BigDecimal.ZERO : invocation.getArgument(0);
            BigDecimal paid = invocation.getArgument(1) == null ? BigDecimal.ZERO : invocation.getArgument(1);
            return amount.subtract(paid);
        });
        when(projectRepository.findByCompany_ComIdOrderByProjectIdDesc(1L)).thenReturn(List.of(project));
        when(receivableRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of());
        when(payableRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(material));
        when(ledgerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of());
        when(projectCostContext.summarize(project)).thenReturn(
                new ProjectCostContext.CostSummary(BigDecimal.ZERO, BigDecimal.ZERO, 1));

        var summary = context.summary(boss);

        assertThat(summary.getCostComplete()).isFalse();
        assertThat(summary.getCostIncompleteItemCount()).isEqualTo(1);
        assertThat(summary.getPayableAmount()).isEqualByComparingTo("1600.00");
        assertThat(summary.getProfitAmount()).isNull();
    }

    @Test
    void gap003MustUseQuoteCostSnapshotWhenSignedContractCostIsZero() {
        FinanceAccessContext access = mock(FinanceAccessContext.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        ReceivableRepository receivableRepository = mock(ReceivableRepository.class);
        PayableRepository payableRepository = mock(PayableRepository.class);
        LedgerRepository ledgerRepository = mock(LedgerRepository.class);
        SettlementRepository settlementRepository = mock(SettlementRepository.class);
        com.th4.edge.decoboon.quote.repository.QuoteLineRepository quoteLineRepository =
                mock(com.th4.edge.decoboon.quote.repository.QuoteLineRepository.class);
        com.th4.edge.decoboon.project.repository.ContractLineRepository contractLineRepository =
                mock(com.th4.edge.decoboon.project.repository.ContractLineRepository.class);
        com.th4.edge.decoboon.project.repository.PurchaseOrderRepository purchaseOrderRepository =
                mock(com.th4.edge.decoboon.project.repository.PurchaseOrderRepository.class);
        ProjectCostContext projectCostContext = new ProjectCostContext(
                quoteLineRepository, contractLineRepository, purchaseOrderRepository);
        FinanceReportContext context = new FinanceReportContext(access, projectRepository,
                receivableRepository, payableRepository, ledgerRepository, settlementRepository,
                projectCostContext);

        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setEmpId(2L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(1003L);
        Project project = new Project();
        project.setProjectId(1004L);
        project.setCompany(company);
        project.setQuote(quote);
        project.setContractAmount(new BigDecimal("50000.00"));
        project.setCost(BigDecimal.ZERO);

        var catalogItem = new com.th4.edge.decoboon.catalog.model.CatalogItem();
        catalogItem.setCostPrice(new BigDecimal("88.00"));
        var quoteLine = new com.th4.edge.decoboon.quote.model.QuoteLine();
        quoteLine.setCatalogItem(catalogItem);
        quoteLine.setType(com.th4.edge.decoboon.catalog.configure.ItemType.Material);
        quoteLine.setPricingMode(com.th4.edge.decoboon.catalog.configure.PricingMethod.MaterialOnly);
        quoteLine.setQuantity(new BigDecimal("2"));
        quoteLine.setCost(new BigDecimal("32000.00"));
        quoteLine.setActualCost(BigDecimal.ZERO);
        var contractLine = new com.th4.edge.decoboon.project.model.ContractLine();
        contractLine.setType(com.th4.edge.decoboon.catalog.configure.ItemType.Material);
        contractLine.setQuantity(new BigDecimal("2"));
        contractLine.setCost(BigDecimal.ZERO);
        contractLine.setSourceQuoteLine(quoteLine);
        Payable material = payable(project, PayableType.Material, "1600.00");

        when(access.requireReader(boss)).thenReturn(boss);
        when(access.resolveProject(boss, 1004L)).thenReturn(project);
        when(access.value(any())).thenAnswer(invocation -> invocation.getArgument(0) == null
                ? BigDecimal.ZERO : invocation.getArgument(0));
        when(access.outstanding(any(), any())).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(0) == null ? BigDecimal.ZERO : invocation.getArgument(0);
            BigDecimal paid = invocation.getArgument(1) == null ? BigDecimal.ZERO : invocation.getArgument(1);
            return amount.subtract(paid);
        });
        contractLine.setContract(contractOf(project));
        when(contractLineRepository.findByContract_Project_ProjectIdInOrderBySortAsc(any())).thenReturn(List.of(contractLine));
        when(purchaseOrderRepository.findByProject_ProjectIdInOrderByPurchaseIdDesc(any())).thenReturn(List.of());
        when(receivableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(List.of());
        when(payableRepository.findByProjectOrderByDueDateAsc(project)).thenReturn(List.of(material));
        when(ledgerRepository.findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(1L, 1004L))
                .thenReturn(List.of());
        when(settlementRepository.findByProject_ProjectIdAndCompany_ComId(1004L, 1L))
                .thenReturn(java.util.Optional.empty());

        FinanceProjectReport report = context.project(boss, 1004L);

        assertThat(report.getEstimatedCost()).isEqualByComparingTo("32000.00");
        assertThat(report.getActualCost()).isEqualByComparingTo("32000.00");
        assertThat(report.getTotalCost()).isEqualByComparingTo("32000.00");
        assertThat(report.getProfitAmount()).isEqualByComparingTo("18000.00");
        assertThat(report.getGrossProfit()).isEqualByComparingTo("18000.00");
        assertThat(report.getGrossMargin()).isEqualByComparingTo("36.00");
        assertThat(report.getProfitAmount()).isNotEqualByComparingTo("48400.00");
        assertThat(report.getGrossMargin()).isNotEqualByComparingTo("97.00");

        quoteLine.setActualCost(new BigDecimal("1600.00"));
        var partialPurchase = new com.th4.edge.decoboon.project.model.PurchaseOrder();
        partialPurchase.setStatus(com.th4.edge.decoboon.project.configure.PurchaseStatus.PartiallyReceived);
        partialPurchase.setQuoteLine(quoteLine);
        partialPurchase.setProject(project);
        when(purchaseOrderRepository.findByProject_ProjectIdInOrderByPurchaseIdDesc(any())).thenReturn(List.of(partialPurchase));

        FinanceProjectReport partialReport = context.project(boss, 1004L);

        assertThat(partialReport.getEstimatedCost()).isEqualByComparingTo("32000.00");
        assertThat(partialReport.getActualCost()).isEqualByComparingTo("1600.00");
        assertThat(partialReport.getCostComplete()).isFalse();
        assertThat(partialReport.getProfitAmount()).isNull();
        assertThat(partialReport.getGrossProfit()).isNull();
        assertThat(partialReport.getGrossMargin()).isNull();

        partialPurchase.setStatus(com.th4.edge.decoboon.project.configure.PurchaseStatus.Received);
        partialPurchase.setQuantity(BigDecimal.ONE);
        partialPurchase.setReceivedQuantity(BigDecimal.ONE);

        FinanceProjectReport partialCoverageReport = context.project(boss, 1004L);

        assertThat(partialCoverageReport.getEstimatedCost()).isEqualByComparingTo("32000.00");
        assertThat(partialCoverageReport.getActualCost()).isEqualByComparingTo("1600.00");
        assertThat(partialCoverageReport.getCostComplete()).isFalse();
        assertThat(partialCoverageReport.getProfitAmount()).isNull();
        assertThat(partialCoverageReport.getGrossMargin()).isNull();

        quoteLine.setActualCost(new BigDecimal("32000.00"));
        partialPurchase.setQuantity(new BigDecimal("2"));
        partialPurchase.setReceivedQuantity(new BigDecimal("2"));

        FinanceProjectReport receivedReport = context.project(boss, 1004L);

        assertThat(receivedReport.getEstimatedCost()).isEqualByComparingTo("32000.00");
        assertThat(receivedReport.getActualCost()).isEqualByComparingTo("32000.00");
        assertThat(receivedReport.getCostComplete()).isTrue();
        assertThat(receivedReport.getProfitAmount()).isEqualByComparingTo("18000.00");
        assertThat(receivedReport.getGrossMargin()).isEqualByComparingTo("36.00");
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

    /** 测试夹具：合同行分组需要 contract.project 关联。 */
    private static com.th4.edge.decoboon.project.model.ProjectContract contractOf(Project project) {
        com.th4.edge.decoboon.project.model.ProjectContract contract =
                new com.th4.edge.decoboon.project.model.ProjectContract();
        contract.setProject(project);
        return contract;
    }
}
