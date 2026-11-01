package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteViewPackageSectionTest {

    @Test
    void groupsPackageLinesByFrozenSectionNameWhenDesignSpaceIsAbsent() {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setVersion(1);
        QuoteLine first = line(quote, 1L, "AUDIT-套餐-区域A", "拆除", 0);
        QuoteLine second = line(quote, 2L, "AUDIT-套餐-区域A", "水电", 1);
        QuoteLine third = line(quote, 3L, "AUDIT-套餐-区域B", "泥木", 2);

        Quote view = new QuoteViewContext().apply(quote, List.of(first, second, third));

        assertThat(view.getRegionGroups()).hasSize(2);
        assertThat(view.getRegionGroups()).extracting(QuoteGroup::getName)
                .containsExactly("AUDIT-套餐-区域A", "AUDIT-套餐-区域B");
        assertThat(view.getRegionGroups()).allSatisfy(group ->
                assertThat(group.getKey()).startsWith("package-section-"));
    }

    private static QuoteLine line(Quote quote, Long itemId, String sectionName, String category, int sort) {
        QuoteLine line = new QuoteLine();
        line.setItemId(itemId);
        line.setQuote(quote);
        line.setPackageSectionNameSnapshot(sectionName);
        line.setCategory(category);
        line.setName(category + "项目");
        line.setType(ItemType.Construction);
        line.setRole(QuoteLineRole.Base);
        line.setIncluded(true);
        line.setQuantity(BigDecimal.ONE);
        line.setAmount(new BigDecimal("100.00"));
        line.setCost(new BigDecimal("50.00"));
        line.setSort(sort);
        return line;
    }
}
