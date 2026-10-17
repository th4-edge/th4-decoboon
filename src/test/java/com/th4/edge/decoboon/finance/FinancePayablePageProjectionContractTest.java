package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** B6-DEF-006：暂估应付必须在读模型中显式可识别。 */
class FinancePayablePageProjectionContractTest {

    @Test
    void payablePageMustExposeEstimatedAndSettledAmount() {
        Properties properties = com.th4.edge.decoboon.finance.service.payables.page.support.Page.class
                .getAnnotation(Properties.class);
        assertThat(properties).isNotNull();
        List<String> fields = Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(Payable.class))
                .findFirst()
                .map(item -> Arrays.asList(item.properties()))
                .orElseGet(List::of);
        assertThat(fields).contains("estimated", "settledAmount", "amount");
    }
}
