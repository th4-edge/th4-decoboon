package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteUnifiedViewPropertiesTest {

    @Test
    void allQuoteReadProvidersShouldExposeServerProjection() {
        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.quote.service.quotes.info.support.Info.class,
                com.th4.edge.decoboon.quote.service.quotes.designerinfo.support.Info.class,
                com.th4.edge.decoboon.project.service.manager.quotes.info.support.Info.class)) {
            assertThat(allowed(provider, Quote.class)).contains("regionGroups", "typeGroups");
            assertThat(allowed(provider, QuoteGroup.class)).contains("groupMode");
            assertThat(allowed(provider, QuoteLine.class)).contains(
                    "typeRowId", "sourceLineIds", "sourceRegionIds", "sourceRegionNames", "sourceCount");
        }
    }

    @Test
    void managerQuoteProjectionShouldNotExposeInternalCostOrFormula() {
        Class<?> manager = com.th4.edge.decoboon.project.service.manager.quotes.info.support.Info.class;

        assertThat(allowed(manager, QuoteGroup.class)).doesNotContain("cost");
        assertThat(allowed(manager, QuoteLine.class)).doesNotContain("cost", "formula", "quantityFormula");
        assertThat(allowed(manager, QuoteComponent.class)).doesNotContain("costAmount", "formula");
        assertThat(allowed(manager, CatalogItem.class)).doesNotContain("costPrice", "formula");
    }

    private List<String> allowed(Class<?> provider, Class<?> model) {
        Properties properties = provider.getAnnotation(Properties.class);
        assertThat(properties).as(provider.getSimpleName()).isNotNull();
        return Arrays.stream(properties.allow())
                .filter(allow -> allow.model().equals(model))
                .flatMap(allow -> Arrays.stream(allow.properties()))
                .toList();
    }
}
