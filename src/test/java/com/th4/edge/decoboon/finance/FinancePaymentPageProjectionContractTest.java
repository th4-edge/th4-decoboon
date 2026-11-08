package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 付款记录必须显式返回归属月份，避免跨月工资付款看起来像重复。 */
class FinancePaymentPageProjectionContractTest {

    @Test
    void paymentPageMustExposePayablePeriod() {
        Properties properties = com.th4.edge.decoboon.finance.service.payments.page.support.Page.class
                .getAnnotation(Properties.class);
        assertThat(properties).isNotNull();
        List<String> fields = Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(Payable.class))
                .findFirst()
                .map(item -> Arrays.asList(item.properties()))
                .orElseGet(List::of);
        assertThat(fields).contains("period");
    }
}
