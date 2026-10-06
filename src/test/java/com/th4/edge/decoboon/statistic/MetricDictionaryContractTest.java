package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.statistic.context.MetricDictionaryContext;
import com.th4.edge.decoboon.statistic.model.MetricDefinition;
import com.th4.edge.decoboon.statistic.model.MetricDictionaryQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** 对外指标口径字典契约：每个数字必须有完整、面向装企老板的说明。 */
class MetricDictionaryContractTest {

    private final MetricDictionaryContext context = new MetricDictionaryContext();

    @Test
    void everyExternalMetricMustHaveCompleteDefinition() {
        List<MetricDefinition> definitions = context.definitions();
        assertThat(definitions).hasSizeGreaterThanOrEqualTo(22);
        assertThat(definitions).extracting(MetricDefinition::getKey).doesNotHaveDuplicates();
        for (MetricDefinition item : definitions) {
            assertThat(item.getKey()).isNotBlank();
            assertThat(item.getName()).isNotBlank();
            assertThat(item.getUserLabel()).isNotBlank();
            assertThat(item.getCategory()).isNotBlank();
            assertThat(item.getDefinition()).isNotBlank();
            assertThat(item.getFormula()).isNotBlank();
            assertThat(item.getSource()).isNotBlank();
            assertThat(item.getTimeScope()).isNotBlank();
            assertThat(item.getDataScope()).isNotBlank();
            assertThat(item.getIncludes()).isNotBlank();
            assertThat(item.getExcludes()).isNotBlank();
            assertThat(item.getMissingValue()).isNotBlank();
            assertThat(item.getExample()).isNotBlank();
            assertThat(item.getUpdatedAt()).isEqualTo("2026-10-07");
            assertThat(item.getStatus()).isEqualTo("verified");
        }
    }

    @Test
    void glossaryCopyMustSpeakPlainChineseWithoutTenantNames() {
        for (MetricDefinition item : context.definitions()) {
            List<String> shown = List.of(
                    item.getName(), item.getUserLabel(), item.getDefinition(), item.getFormula(),
                    item.getSource(), item.getTimeScope(), item.getDataScope(), item.getIncludes(),
                    item.getExcludes(), item.getMissingValue(), item.getExample());
            for (String text : shown) {
                assertThat(text).as("指标 %s 的对外说明不应出现英文：%s", item.getKey(), text)
                        .doesNotContainPattern("[A-Za-z]");
                assertThat(text).as("指标 %s 的对外说明不应出现其他企业名：%s", item.getKey(), text)
                        .doesNotContain("麦鸿");
                assertThat(text).as("指标 %s 的对外说明不应出现技术符号：%s", item.getKey(), text)
                        .doesNotContain("Σ", "null", "false", "≠");
            }
        }
    }

    @Test
    void receiptAndIncomeAndReceivedMustBeDistinct() {
        MetricDefinition receipt = byKey("receiptAmount");
        assertThat(receipt.getSource()).contains("收款");
        assertThat(receipt.getExcludes()).contains("手工");
        assertThat(receipt.getExample()).contains("累计收款");

        MetricDefinition income = byKey("monthIncome");
        assertThat(income.getIncludes()).contains("手工");
        assertThat(income.getExample()).contains("资金收入");

        MetricDefinition received = byKey("receivedAmount");
        assertThat(received.getDefinition()).contains("已经确认收回");
        assertThat(received.getIncludes()).contains("在管");
        assertThat(received.getExample()).contains("已收金额");

        MetricDefinition monthReceipt = byKey("monthReceiptAmount");
        assertThat(monthReceipt.getName()).isEqualTo("本月回款");
        assertThat(monthReceipt.getFormula()).contains("收款");
    }

    @Test
    void incompleteAndZeroBaseRulesMustBeExplicit() {
        assertThat(byKey("grossProfit").getMissingValue()).contains("成本未完整");
        assertThat(byKey("grossMargin").getMissingValue()).contains("成本未完整");
        assertThat(byKey("monthOverMonthRate").getMissingValue()).contains("暂无基期");
        assertThat(byKey("yearOverYearRate").getMissingValue()).contains("暂无基期");
        assertThat(byKey("achievementRate").getMissingValue()).contains("未设目标");
        assertThat(byKey("activeProjectCount").getExcludes()).contains("已经结算");
        assertThat(byKey("cashBalance").getMissingValue()).contains("期初未承接");
    }

    @Test
    void filterByCategoryAndKeysMustWork() {
        MetricDictionaryQuery finance = new MetricDictionaryQuery();
        finance.setCategory("finance");
        assertThat(context.filter(finance)).allMatch(item -> "finance".equals(item.getCategory()));

        MetricDictionaryQuery keys = new MetricDictionaryQuery();
        keys.setKeys(List.of("receiptAmount", "monthIncome"));
        Set<String> result = context.filter(keys).stream().map(MetricDefinition::getKey).collect(Collectors.toSet());
        assertThat(result).containsExactlyInAnyOrder("receiptAmount", "monthIncome");
    }

    private MetricDefinition byKey(String key) {
        return context.definitions().stream().filter(item -> key.equals(item.getKey())).findFirst().orElseThrow();
    }
}
