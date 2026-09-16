package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.project.model.Project;
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
    private final ProjectCostContext context = new ProjectCostContext(quoteLines);

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
    void fixedTotalCostShouldNotMultiplyQuantity() {
        CatalogItem item = item("8000.00", ItemType.Custom, PricingMethod.FixedTotal);
        QuoteLine line = line(item, "3", "0", "0");

        assertThat(context.estimatedCost(line)).isEqualByComparingTo("8000.00");
        assertThat(context.actualCost(line, context.estimatedCost(line))).isEqualByComparingTo("8000.00");
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
