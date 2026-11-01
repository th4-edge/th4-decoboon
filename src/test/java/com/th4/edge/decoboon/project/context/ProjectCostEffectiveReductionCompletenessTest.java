package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** COST-REMOVE：有效 Remove / Quantity 必须按当前有效剩余进入材料完整性分母。 */
class ProjectCostEffectiveReductionCompletenessTest {

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
    private ContractLine contractLine;
    private QuoteLine quoteLine;
    private PurchaseOrder order;
    private PurchaseOrderItem item;

    @BeforeEach
    void setUp() {
        context = new ProjectCostContext(quoteLines, contractLines, purchaseOrders, carryovers, changes,
                purchaseItems, settlementItems, payables, expenses, manualLedger);

        project = new Project();
        project.setProjectId(300L);
        project.setQuote(new Quote());

        CatalogItem catalog = new CatalogItem();
        catalog.setItemId(401L);
        catalog.setType(ItemType.Material);
        catalog.setPricingMode(PricingMethod.MaterialOnly);
        catalog.setCostPrice(new BigDecimal("100.00"));
        catalog.setMaterialPrice(new BigDecimal("200.00"));
        catalog.setManualPrice(BigDecimal.ZERO);

        quoteLine = new QuoteLine();
        quoteLine.setItemId(501L);
        quoteLine.setType(ItemType.Material);
        quoteLine.setCatalogItem(catalog);
        quoteLine.setPricingMode(PricingMethod.MaterialOnly);
        quoteLine.setQuantity(new BigDecimal("10"));
        quoteLine.setCost(new BigDecimal("1000.00"));
        quoteLine.setAmount(new BigDecimal("2000.00"));

        ProjectContract contract = new ProjectContract();
        contract.setContractId(601L);
        contract.setProject(project);
        contractLine = new ContractLine();
        contractLine.setLineId(701L);
        contractLine.setContract(contract);
        contractLine.setSourceQuoteLine(quoteLine);
        contractLine.setType(ItemType.Material);
        contractLine.setQuantity(new BigDecimal("10"));
        contractLine.setCost(new BigDecimal("1000.00"));
        contractLine.setAmount(new BigDecimal("2000.00"));

        order = new PurchaseOrder();
        order.setPurchaseId(801L);
        order.setProject(project);
        order.setQuoteLine(quoteLine);
        order.setStatus(PurchaseStatus.Received);
        order.setQuantity(new BigDecimal("10"));
        order.setReceivedQuantity(new BigDecimal("10"));
        order.setUnitPrice(new BigDecimal("100.00"));
        order.setAmount(new BigDecimal("1000.00"));
        order.setPriceState(SheetPriceState.Priced);

        item = new PurchaseOrderItem();
        item.setPurchaseItemId(901L);
        item.setPurchaseOrder(order);
        item.setProject(project);
        item.setQuantity(new BigDecimal("10"));
        item.setReceivedQuantity(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setAmount(new BigDecimal("1000.00"));
        item.setPriceState(SheetPriceState.Priced);

        when(contractLines.findByContract_Project_ProjectIdInOrderBySortAsc(any())).thenReturn(List.of(contractLine));
        when(purchaseOrders.findByProject_ProjectIdInOrderByPurchaseIdDesc(any())).thenReturn(List.of());
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of());
        when(purchaseItems.findByPurchaseOrder_PurchaseIdIn(any())).thenReturn(List.of());
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any())).thenReturn(List.of());
        when(payables.findByProject_ProjectIdIn(any())).thenReturn(List.of());
        when(expenses.findByProject_ProjectIdInAndStatusIn(any(), any())).thenReturn(List.of());
        when(carryovers.findByProject_ProjectIdIn(any())).thenReturn(List.of());
        when(manualLedger.amountsAll(any())).thenReturn(Map.of(project.getProjectId(),
                ManualLedgerContext.ManualAmounts.ZERO));
    }

    @Test
    void confirmedRemoveBeforePurchaseExcludesMaterialRequirementFromCompleteness() {
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of(
                change(ContractChangeType.Remove, ContractChangeStatus.CustomerConfirmed,
                        new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("-1000.00"))));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("0.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("0.00");
        assertThat(summary.finalCost()).isEqualByComparingTo("0.00");
        assertThat(summary.complete()).isTrue();
        assertThat(summary.finalCostComplete()).isTrue();
        assertThat(summary.incompleteItemCount()).isZero();
    }

    @Test
    void confirmedQuantityReductionUsesRemainingQuantityAsCompletenessDenominator() {
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of(
                change(ContractChangeType.Quantity, ContractChangeStatus.CustomerConfirmed,
                        new BigDecimal("10"), new BigDecimal("4"), new BigDecimal("-600.00"))));
        order.setQuantity(new BigDecimal("4"));
        order.setReceivedQuantity(new BigDecimal("4"));
        item.setQuantity(new BigDecimal("4"));
        item.setReceivedQuantity(new BigDecimal("4"));
        item.setPriceState(SheetPriceState.Settled);
        when(purchaseOrders.findByProject_ProjectIdInOrderByPurchaseIdDesc(any())).thenReturn(List.of(order));
        when(purchaseItems.findByPurchaseOrder_PurchaseIdIn(any())).thenReturn(List.of(item));
        when(settlementItems.findByPurchaseItem_PurchaseItemIdIn(any()))
                .thenReturn(List.of(settlement(new BigDecimal("4"), new BigDecimal("110.00"))));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("400.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("440.00");
        assertThat(summary.finalCost()).isEqualByComparingTo("440.00");
        assertThat(summary.complete()).isTrue();
        assertThat(summary.finalCostComplete()).isTrue();
        assertThat(summary.incompleteItemCount()).isZero();
    }

    @Test
    void pendingQuantityChangeMustNotReduceCompletenessDenominator() {
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of(
                change(ContractChangeType.Quantity, ContractChangeStatus.Pending,
                        new BigDecimal("10"), new BigDecimal("4"), new BigDecimal("-600.00"))));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.complete()).isFalse();
        assertThat(summary.finalCostComplete()).isFalse();
    }

    @Test
    void confirmedQuantityReductionWithoutRemainingPurchaseRemainsIncomplete() {
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of(
                change(ContractChangeType.Quantity, ContractChangeStatus.CustomerConfirmed,
                        new BigDecimal("10"), new BigDecimal("4"), new BigDecimal("-600.00"))));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.complete()).isFalse();
        assertThat(summary.finalCostComplete()).isFalse();
    }

    @Test
    void replaceDoesNotSilentlyDropSourceRequirementBeforeTargetModelIsComplete() {
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of(
                change(ContractChangeType.Replace, ContractChangeStatus.CustomerConfirmed,
                        new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("-1000.00"))));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        // Replace 的完整处理必须同时计入目标需求；本批不把来源单独关闭成“完整”。
        assertThat(summary.complete()).isFalse();
        assertThat(summary.finalCostComplete()).isFalse();
    }

    @Test
    void removeAfterReceiptRemainsIncompleteUntilReturnOrAdjustmentClosesExcess() {
        when(changes.findEffectiveByProjectIds(any(), any())).thenReturn(List.of(
                change(ContractChangeType.Remove, ContractChangeStatus.CustomerConfirmed,
                        new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("-1000.00"))));
        when(purchaseOrders.findByProject_ProjectIdInOrderByPurchaseIdDesc(any())).thenReturn(List.of(order));
        when(purchaseItems.findByPurchaseOrder_PurchaseIdIn(any())).thenReturn(List.of(item));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.actualCost()).isEqualByComparingTo("1000.00");
        assertThat(summary.complete()).isFalse();
        assertThat(summary.finalCostComplete()).isFalse();
    }

    private ContractChangeItem change(ContractChangeType type, ContractChangeStatus status,
                                      BigDecimal oldQuantity, BigDecimal newQuantity, BigDecimal costDelta) {
        ContractChange change = new ContractChange();
        change.setProject(project);
        change.setStatus(status);
        ContractChangeItem item = new ContractChangeItem();
        item.setChange(change);
        item.setSourceLine(contractLine);
        item.setChangeType(type);
        item.setOldQuantity(oldQuantity);
        item.setNewQuantity(newQuantity);
        item.setCostDelta(costDelta);
        return item;
    }

    private MaterialSettlementItem settlement(BigDecimal quantity, BigDecimal price) {
        MaterialSettlement settlement = new MaterialSettlement();
        settlement.setStatus(SettlementStatus.Confirmed);
        MaterialSettlementItem line = new MaterialSettlementItem();
        line.setSettlement(settlement);
        line.setPurchaseItem(item);
        line.setQuantity(quantity);
        line.setSettlementPrice(price);
        line.setAmount(quantity.multiply(price));
        return line;
    }
}
