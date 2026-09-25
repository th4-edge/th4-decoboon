package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.decoboon.carryover.repository.CarryoverCostSnapshotRepository;
import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 采购价、报价快照和实际成本回算口径。 */
class ProjectCostContextTest {

    private final QuoteLineRepository quoteLines = mock(QuoteLineRepository.class);
    private final ContractLineRepository contractLines = mock(ContractLineRepository.class);
    private final PurchaseOrderRepository purchaseOrders = mock(PurchaseOrderRepository.class);
    private final ProjectCostContext context = new ProjectCostContext(quoteLines, contractLines, purchaseOrders);

    @Test
    void shouldRecalculateActualCostWhenPurchasePriceChanges() {
        CatalogItem item = item("68.00", ItemType.Material, PricingMethod.MaterialAndManual);
        QuoteLine line = line(item, "2", "100.00", "0");
        Project project = project(line);

        ProjectCostContext.CostSummary first = context.summarize(project);
        assertThat(first.estimatedCost()).isEqualByComparingTo("100.00");
        assertThat(first.actualCost()).isEqualByComparingTo("136.00");
        assertThat(first.complete()).isTrue();

        item.setCostPrice(new BigDecimal("80.00"));
        ProjectCostContext.CostSummary changed = context.summarize(project);
        assertThat(changed.estimatedCost()).isEqualByComparingTo("100.00");
        assertThat(changed.actualCost()).isEqualByComparingTo("160.00");
    }

    @Test
    void shouldPreferActualCostAndMarkMissingCatalogCostIncomplete() {
        CatalogItem priced = item("68.00", ItemType.Custom, PricingMethod.MaterialAndManual);
        QuoteLine actualLine = line(priced, "2", "100.00", "150.00");
        QuoteLine missingLine = line(item("0.00", ItemType.Appliance, PricingMethod.MaterialOnly), "1", "0", "0");
        Project project = project(actualLine, missingLine);

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("100.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("150.00");
        assertThat(summary.incompleteItemCount()).isEqualTo(1);
        assertThat(summary.complete()).isFalse();
    }

    @Test
    void shouldMergeCarryoverCostSnapshot() {
        CarryoverCostSnapshotRepository snapshots = mock(CarryoverCostSnapshotRepository.class);
        ProjectCostContext carryoverContext = new ProjectCostContext(
                quoteLines, contractLines, purchaseOrders, snapshots);
        Project project = new Project();
        project.setProjectId(88L);
        project.setBusinessOrigin(BusinessOrigin.Carryover);
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(880L);
        project.setQuote(quote);
        CarryoverCostSnapshot snapshot = new CarryoverCostSnapshot();
        snapshot.setTotalCost(new BigDecimal("12000.00"));
        snapshot.setCostStatus(CarryoverCostStatus.Complete);
        when(snapshots.findByProject_ProjectId(88L)).thenReturn(java.util.Optional.of(snapshot));

        ProjectCostContext.CostSummary summary = carryoverContext.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("12000.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("12000.00");
        assertThat(summary.complete()).isTrue();
    }

    @Test
    void incompleteCarryoverCostSnapshotMustKeepProfitIncomplete() {
        CarryoverCostSnapshotRepository snapshots = mock(CarryoverCostSnapshotRepository.class);
        ProjectCostContext carryoverContext = new ProjectCostContext(
                quoteLines, contractLines, purchaseOrders, snapshots);
        Project project = new Project();
        project.setProjectId(89L);
        project.setBusinessOrigin(BusinessOrigin.Carryover);
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(890L);
        project.setQuote(quote);
        CarryoverCostSnapshot snapshot = new CarryoverCostSnapshot();
        snapshot.setTotalCost(new BigDecimal("12000.00"));
        snapshot.setCostStatus(CarryoverCostStatus.TotalOnly);
        when(snapshots.findByProject_ProjectId(89L)).thenReturn(java.util.Optional.of(snapshot));

        ProjectCostContext.CostSummary summary = carryoverContext.summarize(project);

        assertThat(summary.actualCost()).isEqualByComparingTo("12000.00");
        assertThat(summary.complete()).isFalse();
    }

    @Test
    void fixedTotalCostShouldNotMultiplyQuantity() {
        CatalogItem item = item("8000.00", ItemType.Custom, PricingMethod.FixedTotal);
        QuoteLine line = line(item, "3", "0", "0");

        assertThat(context.estimatedCost(line)).isEqualByComparingTo("8000.00");
        assertThat(context.actualCost(line, context.estimatedCost(line))).isEqualByComparingTo("8000.00");
    }

    @Test
    void quoteWithoutItemsMustBeIncompleteEvenWhenProjectCostIsZero() {
        Project project = new Project();
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(100L);
        project.setQuote(quote);
        when(quoteLines.findByQuote(quote)).thenReturn(List.of());

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("0.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("0.00");
        assertThat(summary.incompleteItemCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.complete()).isFalse();
    }


    @Test
    void signedContractCostMustWinAfterCatalogCostChanges() {
        Project project = new Project();
        project.setProjectId(77L);
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(777L);
        project.setQuote(quote);
        com.th4.edge.decoboon.project.model.ContractLine line =
                new com.th4.edge.decoboon.project.model.ContractLine();
        line.setType(ItemType.Material);
        line.setCost(new BigDecimal("188.00"));
        when(contractLines.findByContract_Project_ProjectIdOrderBySortAsc(77L)).thenReturn(List.of(line));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("188.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("188.00");
        assertThat(summary.complete()).isTrue();
    }

    @Test
    void signedContractMissingCostMustFallbackToQuoteSnapshot() {
        Project project = new Project();
        project.setProjectId(78L);
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(778L);
        project.setQuote(quote);
        QuoteLine source = line(item("88.00", ItemType.Material, PricingMethod.MaterialOnly),
                "2", "32000.00", "0");
        com.th4.edge.decoboon.project.model.ContractLine contractLine =
                new com.th4.edge.decoboon.project.model.ContractLine();
        contractLine.setType(ItemType.Material);
        contractLine.setCost(BigDecimal.ZERO);
        contractLine.setSourceQuoteLine(source);
        when(contractLines.findByContract_Project_ProjectIdOrderBySortAsc(78L))
                .thenReturn(List.of(contractLine));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("32000.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("32000.00");
        assertThat(summary.incompleteItemCount()).isZero();
        assertThat(summary.complete()).isTrue();
    }

    @Test
    void partiallyReceivedPurchaseMustKeepCostIncomplete() {
        Project project = new Project();
        project.setProjectId(80L);
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(780L);
        project.setQuote(quote);
        QuoteLine source = line(item("88.00", ItemType.Material, PricingMethod.MaterialOnly),
                "2", "32000.00", "1600.00");
        com.th4.edge.decoboon.project.model.ContractLine contractLine =
                new com.th4.edge.decoboon.project.model.ContractLine();
        contractLine.setType(ItemType.Material);
        contractLine.setCost(BigDecimal.ZERO);
        contractLine.setSourceQuoteLine(source);
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setStatus(com.th4.edge.decoboon.project.configure.PurchaseStatus.PartiallyReceived);
        purchaseOrder.setQuoteLine(source);
        when(contractLines.findByContract_Project_ProjectIdOrderBySortAsc(80L))
                .thenReturn(List.of(contractLine));
        when(purchaseOrders.findByProjectOrderByPurchaseIdDesc(project)).thenReturn(List.of(purchaseOrder));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("32000.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("1600.00");
        assertThat(summary.incompleteItemCount()).isEqualTo(1);
        assertThat(summary.complete()).isFalse();
    }

    @Test
    void fullyReceivedPurchaseWithPartialCoverageMustRemainIncomplete() {
        Project project = new Project();
        project.setProjectId(81L);
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(781L);
        project.setQuote(quote);
        QuoteLine source = line(item("88.00", ItemType.Material, PricingMethod.MaterialOnly),
                "2", "32000.00", "1600.00");
        com.th4.edge.decoboon.project.model.ContractLine contractLine =
                new com.th4.edge.decoboon.project.model.ContractLine();
        contractLine.setType(ItemType.Material);
        contractLine.setQuantity(new BigDecimal("2"));
        contractLine.setCost(BigDecimal.ZERO);
        contractLine.setSourceQuoteLine(source);
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setStatus(com.th4.edge.decoboon.project.configure.PurchaseStatus.Received);
        purchaseOrder.setQuoteLine(source);
        purchaseOrder.setQuantity(BigDecimal.ONE);
        purchaseOrder.setReceivedQuantity(BigDecimal.ONE);
        when(contractLines.findByContract_Project_ProjectIdOrderBySortAsc(81L))
                .thenReturn(List.of(contractLine));
        when(purchaseOrders.findByProjectOrderByPurchaseIdDesc(project)).thenReturn(List.of(purchaseOrder));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("32000.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("1600.00");
        assertThat(summary.incompleteItemCount()).isEqualTo(1);
        assertThat(summary.complete()).isFalse();
    }

    @Test
    void multipleReceivedPurchasesMustFullyCoverQuantity() {
        Project project = new Project();
        project.setProjectId(82L);
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(782L);
        project.setQuote(quote);
        QuoteLine source = line(item("88.00", ItemType.Material, PricingMethod.MaterialOnly),
                "2", "32000.00", "0");
        com.th4.edge.decoboon.project.model.ContractLine contractLine =
                new com.th4.edge.decoboon.project.model.ContractLine();
        contractLine.setType(ItemType.Material);
        contractLine.setQuantity(new BigDecimal("2"));
        contractLine.setCost(BigDecimal.ZERO);
        contractLine.setSourceQuoteLine(source);
        PurchaseOrder first = new PurchaseOrder();
        first.setStatus(com.th4.edge.decoboon.project.configure.PurchaseStatus.Received);
        first.setQuoteLine(source);
        first.setQuantity(BigDecimal.ONE);
        first.setReceivedQuantity(BigDecimal.ONE);
        PurchaseOrder second = new PurchaseOrder();
        second.setStatus(com.th4.edge.decoboon.project.configure.PurchaseStatus.Received);
        second.setQuoteLine(source);
        second.setQuantity(BigDecimal.ONE);
        second.setReceivedQuantity(BigDecimal.ONE);
        when(contractLines.findByContract_Project_ProjectIdOrderBySortAsc(82L))
                .thenReturn(List.of(contractLine));
        when(purchaseOrders.findByProjectOrderByPurchaseIdDesc(project))
                .thenReturn(List.of(first, second));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("32000.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("32000.00");
        assertThat(summary.incompleteItemCount()).isZero();
        assertThat(summary.complete()).isTrue();
    }

    @Test
    void signedContractAndQuoteSnapshotMissingMustRemainIncomplete() {
        Project project = new Project();
        project.setProjectId(79L);
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(779L);
        project.setQuote(quote);
        QuoteLine source = line(item("0.00", ItemType.Material, PricingMethod.MaterialAndManual),
                "1", "0", "0");
        com.th4.edge.decoboon.project.model.ContractLine contractLine =
                new com.th4.edge.decoboon.project.model.ContractLine();
        contractLine.setType(ItemType.Material);
        contractLine.setCost(BigDecimal.ZERO);
        contractLine.setSourceQuoteLine(source);
        when(contractLines.findByContract_Project_ProjectIdOrderBySortAsc(79L))
                .thenReturn(List.of(contractLine));

        ProjectCostContext.CostSummary summary = context.summarize(project);

        assertThat(summary.estimatedCost()).isEqualByComparingTo("0.00");
        assertThat(summary.actualCost()).isEqualByComparingTo("0.00");
        assertThat(summary.incompleteItemCount()).isEqualTo(1);
        assertThat(summary.complete()).isFalse();
    }

    private Project project(QuoteLine... lines) {
        Project project = new Project();
        project.setCost(BigDecimal.ZERO);
        var quote = new com.th4.edge.decoboon.quote.model.Quote();
        quote.setQuoteId(99L);
        project.setQuote(quote);
        when(quoteLines.findByQuote(quote)).thenReturn(List.of(lines));
        return project;
    }

    private QuoteLine line(CatalogItem item, String quantity, String snapshotCost, String actualCost) {
        QuoteLine line = new QuoteLine();
        line.setCatalogItem(item);
        line.setType(item.getType());
        line.setPricingMode(item.getPricingMode());
        line.setQuantity(new BigDecimal(quantity));
        line.setCost(new BigDecimal(snapshotCost));
        line.setActualCost(new BigDecimal(actualCost));
        return line;
    }

    private CatalogItem item(String cost, ItemType type, PricingMethod pricingMode) {
        CatalogItem item = new CatalogItem();
        item.setItemId((long) (Math.random() * 100000));
        item.setCostPrice(new BigDecimal(cost));
        item.setType(type);
        item.setPricingMode(pricingMode);
        return item;
    }
}
