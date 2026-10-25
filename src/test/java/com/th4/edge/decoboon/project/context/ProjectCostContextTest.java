package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.ManualLedgerContext;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.material.configure.SettlementStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.model.MaterialSettlement;
import com.th4.edge.decoboon.material.model.MaterialSettlementItem;
import com.th4.edge.decoboon.material.repository.MaterialSettlementItemRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderItemRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** COST-D2：P1/P2/P3、分类应计、完整性与防双计。 */
class ProjectCostContextTest {

    private final QuoteLineRepository quoteLines = mock(QuoteLineRepository.class);
    private final ContractLineRepository contractLines = mock(ContractLineRepository.class);
    private final PurchaseOrderRepository purchaseOrders = mock(PurchaseOrderRepository.class);
    private final CarryoverCostSnapshotRepository carryovers = mock(CarryoverCostSnapshotRepository.class);
    private final ContractChangeItemRepository changes = mock(ContractChangeItemRepository.class);
    private final PurchaseOrderItemRepository purchaseItems = mock(PurchaseOrderItemRepository.class);
    private final MaterialSettlementItemRepository settlementItems = mock(MaterialSettlementItemRepository.class);
    private final PayableRepository payables = mock(PayableRepository.class);
    private final ExpenseRepository expenses = mock(ExpenseRepository.class);
    private final ManualLedgerContext manualLedger = mock(ManualLedgerContext.class);

    private ProjectCostContext context;
    private Project project;
    private QuoteLine quoteLine;
    private ContractLine contractLine;
    private PurchaseOrder order;
    private PurchaseOrderItem item;

    @BeforeEach
    void setUp() {
        context = new ProjectCostContext(quoteLines, contractLines, purchaseOrders, carryovers, changes,
                purchaseItems, settlementItems, payables, expenses, manualLedger);

        Company company = new Company();
        company.setComId(10L);
        Quote quote = new Quote();
        quote.setQuoteId(20L);
        project = new Project();
        project.setProjectId(30L);
        project.setCompany(company);
        project.setQuote(quote);
        project.setBusinessOrigin(BusinessOrigin.New);

        CatalogItem catalog = new CatalogItem();
        catalog.setItemId(40L);
        catalog.setType(ItemType.Material);
        catalog.setPricingMode(PricingMethod.MaterialOnly);
        catalog.setCostPrice(new BigDecimal("100.00"));
        catalog.setMaterialPrice(new BigDecimal("200.00"));
        catalog.setManualPrice(BigDecimal.ZERO);

        quoteLine = new QuoteLine();
        quoteLine.setItemId(50L);
        quoteLine.setType(ItemType.Material);
        quoteLine.setCatalogItem(catalog);
        quoteLine.setPricingMode(PricingMethod.MaterialOnly);
        quoteLine.setQuantity(new BigDecimal("10"));
        quoteLine.setCost(new BigDecimal("1000.00"));
        quoteLine.setAmount(new BigDecimal("2000.00"));

        ProjectContract contract = new ProjectContract();
        contract.setProject(project);
        contractLine = new ContractLine();
        contractLine.setContract(contract);
        contractLine.setSourceQuoteLine(quoteLine);
        contractLine.setType(ItemType.Material);
        contractLine.setQuantity(new BigDecimal("10"));
        contractLine.setCost(new BigDecimal("1000.00"));
        contractLine.setAmount(new BigDecimal("2000.00"));

        order = new PurchaseOrder();
        order.setPurchaseId(60L);
        order.setProject(project);
        order.setQuoteLine(quoteLine);
        order.setStatus(PurchaseStatus.Received);
        order.setQuantity(new BigDecimal("10"));
        order.setReceivedQuantity(new BigDecimal("10"));
        order.setUnitPrice(new BigDecimal("100.00"));
        order.setAmount(new BigDecimal("1000.00"));
        order.setPriceState(SheetPriceState.Priced);

        item = new PurchaseOrderItem();
        item.setPurchaseItemId(70L);
        item.setPurchaseOrder(order);
        item.setProject(project);
        item.setQuantity(new BigDecimal("10"));
        item.setReceivedQuantity(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setAmount(new BigDecimal("1000.00"));
        item.setPriceState(SheetPriceState.Priced);

        when(contractLines.findByContract_Project_ProjectIdInOrderBySortAsc(any())).thenReturn(List.of(contractLine));
        when(purchaseOrders.findByProject_ProjectIdInOrderByPurchaseIdDesc(any())).thenReturn(List.of(order));
        when(purchaseItems.findByPurchaseOrder_PurchaseIdIn(any())).thenReturn(List.of(item));
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any())).thenReturn(List.of());
        when(payables.findByProject_ProjectIdIn(any())).thenReturn(List.of());
        when(expenses.findByProject_ProjectIdInAndStatusIn(any(), any())).thenReturn(List.of());
        when(carryovers.findByProject_ProjectIdIn(any())).thenReturn(List.of());
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of());
        when(manualLedger.amountsAll(any())).thenReturn(Map.of(project.getProjectId(),
                ManualLedgerContext.ManualAmounts.ZERO));
    }

    @Test
    void p1OnlyPopulatesEstimatedAndNeverFallsBackIntoActualOrFinal() {
        when(purchaseOrders.findByProject_ProjectIdInOrderByPurchaseIdDesc(any())).thenReturn(List.of());

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("1000.00");
        assertThat(summary.estimatedCostComplete()).isTrue();
        assertThat(summary.actualCost()).isEqualByComparingTo("0.00");
        assertThat(summary.complete()).isFalse();
        assertThat(summary.finalCost()).isEqualByComparingTo("0.00");
        assertThat(summary.finalCostComplete()).isFalse();
        assertThat(summary.projection().getActualCost()).isEqualByComparingTo("0.00");
    }

    @Test
    void pricedReceivedP2IsDynamicProvisionalButNeverFinal() {
        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.materialCost()).isEqualByComparingTo("1000.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("1000.00");
        assertThat(summary.complete()).isTrue();
        assertThat(summary.dynamicCostProvisional()).isTrue();
        assertThat(summary.projection().dynamicCostProvisional()).isTrue();
        assertThat(summary.finalCost()).isEqualByComparingTo("0.00");
        assertThat(summary.finalCostComplete()).isFalse();
    }

    @Test
    void confirmedSettlementP3IsTheOnlyFinalMaterialCost() {
        item.setPriceState(SheetPriceState.Settled);
        order.setPriceState(SheetPriceState.Settled);
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any()))
                .thenReturn(List.of(settlement(item, "10", "110.00")));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.actualCost()).isEqualByComparingTo("1100.00");
        assertThat(summary.finalCost()).isEqualByComparingTo("1100.00");
        assertThat(summary.materialCost()).isEqualByComparingTo("1100.00");
        assertThat(summary.complete()).isTrue();
        assertThat(summary.finalCostComplete()).isTrue();
        assertThat(summary.dynamicCostProvisional()).isFalse();
    }

    @Test
    void mixedP3AndP2KeepsP2OutOfFinalCost() {
        item.setPriceState(SheetPriceState.Mixed);
        order.setPriceState(SheetPriceState.Mixed);
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any()))
                .thenReturn(List.of(settlement(item, "6", "110.00")));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.actualCost()).isEqualByComparingTo("1060.00");
        assertThat(summary.finalCost()).isEqualByComparingTo("660.00");
        assertThat(summary.dynamicCostProvisional()).isTrue();
        assertThat(summary.complete()).isTrue();
        assertThat(summary.finalCostComplete()).isFalse();
    }

    @Test
    void partialArrivalAndUnpricedArrivalCannotProduceCompleteDynamicCost() {
        item.setReceivedQuantity(new BigDecimal("5"));
        order.setReceivedQuantity(new BigDecimal("5"));
        ProjectCostContext.CostSummary partial = context.summarize(project);
        assertThat(partial.complete()).isFalse();

        item.setReceivedQuantity(new BigDecimal("10"));
        item.setUnitPrice(BigDecimal.ZERO);
        item.setPriceState(SheetPriceState.Unpriced);
        order.setReceivedQuantity(new BigDecimal("10"));
        order.setUnitPrice(BigDecimal.ZERO);
        order.setPriceState(SheetPriceState.Unpriced);
        ProjectCostContext.CostSummary unpriced = context.summarize(project);
        assertThat(unpriced.complete()).isFalse();
        assertThat(unpriced.projection().getProfitAmount()).isNull();
    }

    @Test
    void classifiesOnlyConfirmedAccrualSourcesAndExcludesMaterialSupplierOtherPayables() {
        item.setPriceState(SheetPriceState.Settled);
        order.setPriceState(SheetPriceState.Settled);
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any()))
                .thenReturn(List.of(settlement(item, "10", "110.00")));
        when(payables.findByProject_ProjectIdIn(any())).thenReturn(List.of(
                payable(PayableType.Salary, "200.00"),
                payable(PayableType.EmployeeSalary, "300.00"),
                payable(PayableType.EmployeeCommission, "50.00"),
                payable(PayableType.Project, "70.00"),
                payable(PayableType.Material, "999.00"),
                payable(PayableType.Supplier, "888.00"),
                payable(PayableType.Other, "777.00"),
                payable(PayableType.Expense, "666.00")));
        Expense projectExpense = expense(project, ExpenseStatus.Approved, "40.00");
        Expense enterpriseExpense = expense(null, ExpenseStatus.Approved, "500.00");
        when(expenses.findByProject_ProjectIdInAndStatusIn(any(), any()))
                .thenReturn(List.of(projectExpense, enterpriseExpense));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.materialCost()).isEqualByComparingTo("1100.00");
        assertThat(summary.workerLaborCost()).isEqualByComparingTo("200.00");
        assertThat(summary.employeeSalaryCost()).isEqualByComparingTo("300.00");
        assertThat(summary.employeeCommissionCost()).isEqualByComparingTo("50.00");
        assertThat(summary.projectExpenseCost()).isEqualByComparingTo("40.00");
        assertThat(summary.otherProjectAccruedCost()).isEqualByComparingTo("70.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("1760.00");
        assertThat(summary.complete()).isTrue();
        assertThat(summary.finalCostComplete()).isTrue();
    }

    @Test
    void projectExpenseAndManualIncidentalCostConflictNeverDoubleCounts() {
        item.setPriceState(SheetPriceState.Settled);
        order.setPriceState(SheetPriceState.Settled);
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any()))
                .thenReturn(List.of(settlement(item, "10", "110.00")));
        when(expenses.findByProject_ProjectIdInAndStatusIn(any(), any()))
                .thenReturn(List.of(expense(project, ExpenseStatus.Approved, "40.00")));
        when(manualLedger.amountsAll(any())).thenReturn(Map.of(project.getProjectId(),
                new ManualLedgerContext.ManualAmounts(BigDecimal.ZERO, new BigDecimal("30.00"))));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.projectExpenseCost()).isEqualByComparingTo("40.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("1140.00");
        assertThat(summary.complete()).isFalse();
        assertThat(summary.finalCostComplete()).isFalse();
    }

    @Test
    void enterpriseExpenseNeverEntersProjectCost() {
        item.setPriceState(SheetPriceState.Settled);
        order.setPriceState(SheetPriceState.Settled);
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any()))
                .thenReturn(List.of(settlement(item, "10", "110.00")));
        when(expenses.findByProject_ProjectIdInAndStatusIn(any(), any()))
                .thenReturn(List.of(expense(null, ExpenseStatus.Approved, "500.00")));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.projectExpenseCost()).isEqualByComparingTo("0.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("1100.00");
        assertThat(summary.complete()).isTrue();
    }

    @Test
    void carryoverSnapshotIsAddedOnceToDynamicAndFinalCost() {
        project.setBusinessOrigin(BusinessOrigin.Carryover);
        item.setPriceState(SheetPriceState.Settled);
        order.setPriceState(SheetPriceState.Settled);
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any()))
                .thenReturn(List.of(settlement(item, "10", "110.00")));
        CarryoverCostSnapshot snapshot = new CarryoverCostSnapshot();
        snapshot.setProject(project);
        snapshot.setTotalCost(new BigDecimal("5000.00"));
        snapshot.setCostStatus(CarryoverCostStatus.Complete);
        when(carryovers.findByProject_ProjectIdIn(any())).thenReturn(List.of(snapshot));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.carryoverCost()).isEqualByComparingTo("5000.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("6100.00");
        assertThat(summary.finalCost()).isEqualByComparingTo("6100.00");
        assertThat(summary.complete()).isTrue();
        assertThat(summary.finalCostComplete()).isTrue();
    }

    @Test
    void effectiveChangeDeltaOnlyChangesEstimatedCost() {
        ContractChange change = new ContractChange();
        change.setProject(project);
        change.setStatus(ContractChangeStatus.CustomerConfirmed);
        ContractChangeItem delta = new ContractChangeItem();
        delta.setChange(change);
        delta.setChangeType(ContractChangeType.Add);
        delta.setCostDelta(new BigDecimal("100.00"));
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of(delta));
        when(purchaseOrders.findByProject_ProjectIdInOrderByPurchaseIdDesc(any())).thenReturn(List.of());

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("1100.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("0.00");
        assertThat(summary.finalCost()).isEqualByComparingTo("0.00");
    }

    private Payable payable(PayableType type, String amount) {
        Payable payable = new Payable();
        payable.setCompany(project.getCompany());
        payable.setProject(project);
        payable.setType(type);
        payable.setAmount(new BigDecimal(amount));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setBusinessOrigin(BusinessOrigin.New);
        payable.setEstimated(Boolean.FALSE);
        return payable;
    }

    private Expense expense(Project owner, ExpenseStatus status, String amount) {
        Expense expense = new Expense();
        expense.setProject(owner);
        expense.setStatus(status);
        expense.setAmount(new BigDecimal(amount));
        return expense;
    }

    private MaterialSettlementItem settlement(PurchaseOrderItem purchaseItem, String quantity, String price) {
        MaterialSettlement settlement = new MaterialSettlement();
        settlement.setStatus(SettlementStatus.Confirmed);
        MaterialSettlementItem line = new MaterialSettlementItem();
        line.setSettlement(settlement);
        line.setPurchaseItem(purchaseItem);
        line.setQuantity(new BigDecimal(quantity));
        line.setSettlementPrice(new BigDecimal(price));
        line.setAmount(new BigDecimal(quantity).multiply(new BigDecimal(price)));
        return line;
    }
}
