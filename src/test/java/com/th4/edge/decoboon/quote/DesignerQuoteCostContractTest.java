package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DesignerQuoteCostContractTest {

    @Test
    void designerQuotePageAndInfoMustNotExposeCostProperties() {
        assertNoCost(com.th4.edge.decoboon.quote.service.quotes.designerpage.support.Page.class);
        assertNoCost(com.th4.edge.decoboon.quote.service.quotes.designerinfo.support.Info.class);
    }

    private void assertNoCost(Class<?> provider) {
        Properties properties = provider.getAnnotation(Properties.class);
        assertThat(properties).isNotNull();
        assertThat(Arrays.stream(properties.allow())
                .flatMap(allow -> Arrays.stream(allow.properties()))
                .toList()).doesNotContain("cost", "costComplete", "costAmount", "costPrice");
        assertThat(Arrays.stream(properties.allow())
                .map(AllowProperties::model)
                .toList()).contains(Quote.class);
    }
}
