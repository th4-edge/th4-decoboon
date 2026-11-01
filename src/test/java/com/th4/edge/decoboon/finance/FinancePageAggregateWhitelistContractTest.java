package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 财务读模型经项目->房屋关联时必须显式开放房屋摘要，避免序列化器截断整个响应。 */
class FinancePageAggregateWhitelistContractTest {

    private static final List<String> PROPERTY_SUMMARY_FIELDS = List.of(
            "propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status");

    @Test
    void financePagesMustWhitelistNestedPropertySummary() {
        List<Class<?>> providers = List.of(
                com.th4.edge.decoboon.finance.service.summary.report.support.Report.class,
                com.th4.edge.decoboon.finance.service.receivables.page.support.Page.class,
                com.th4.edge.decoboon.finance.service.receipts.page.support.Page.class,
                com.th4.edge.decoboon.finance.service.payables.page.support.Page.class,
                com.th4.edge.decoboon.finance.service.payments.page.support.Page.class,
                com.th4.edge.decoboon.finance.service.ledgers.page.support.Page.class,
                com.th4.edge.decoboon.finance.service.refunds.page.support.Page.class,
                com.th4.edge.decoboon.finance.service.settlements.page.support.Page.class);

        assertThat(providers).allSatisfy(provider -> {
            Properties properties = provider.getAnnotation(Properties.class);
            assertThat(properties).as(provider.getName()).isNotNull();
            assertThat(properties.allow()).anySatisfy(item -> {
                assertThat(item.model()).isEqualTo(Property.class);
                assertThat(Arrays.asList(item.properties())).containsAll(PROPERTY_SUMMARY_FIELDS);
            });
        });
    }

    @Test
    void quoteUpdateMustWhitelistNestedCustomerSummary() {
        Properties properties = com.th4.edge.decoboon.quote.service.quotes.update.support.Update.class
                .getAnnotation(Properties.class);
        assertThat(properties).isNotNull();
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Customer.class);
            assertThat(Arrays.asList(item.properties())).contains("custId", "name", "telNo", "community", "addr", "area");
        });
    }
    @Test
    void payablePageMustWhitelistNestedPartnerSummary() {
        Properties properties = com.th4.edge.decoboon.finance.service.payables.page.support.Page.class
                .getAnnotation(Properties.class);
        assertThat(properties).isNotNull();
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Partner.class);
            assertThat(Arrays.asList(item.properties()))
                    .contains("partnerId", "name", "type", "telNo", "settlementMode", "settlementDays", "cutoffDay")
                    .doesNotContain("company", "createTime");
        });
    }

    @Test
    void settlementMutationsMustWhitelistNestedPropertySummary() {
        List<Class<?>> providers = List.of(
                com.th4.edge.decoboon.finance.service.settlements.save.support.Save.class,
                com.th4.edge.decoboon.finance.service.settlements.update.support.Update.class);
        assertThat(providers).allSatisfy(provider -> {
            Properties properties = provider.getAnnotation(Properties.class);
            assertThat(properties).as(provider.getName()).isNotNull();
            assertThat(properties.allow()).anySatisfy(item -> {
                assertThat(item.model()).isEqualTo(Property.class);
                assertThat(Arrays.asList(item.properties())).containsAll(PROPERTY_SUMMARY_FIELDS);
            });
        });
    }

    @Test
    void projectReportMustExposeSettlementAmountsAndCostCompleteness() {
        Properties properties = com.th4.edge.decoboon.finance.service.projects.report.support.Report.class
                .getAnnotation(Properties.class);
        assertThat(properties).isNotNull();
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Settlement.class);
            assertThat(Arrays.asList(item.properties()))
                    .contains("finalAmount", "retentionAmount", "retentionRate", "totalCost", "profitAmount",
                            "costComplete", "costIncompleteCount", "grossMargin", "status");
        });
    }

}
