package com.th4.edge.decoboon.document;

import com.th4.edge.decoboon.document.context.VoucherAmountWords;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** 凭证金额大写规则（冻结清单第 12 节）。 */
class VoucherAmountWordsTest {

    @Test
    void shouldConvertAccordingToFrozenRules() {
        assertThat(VoucherAmountWords.of(new BigDecimal("40.00"))).isEqualTo("肆拾元整");
        assertThat(VoucherAmountWords.of(new BigDecimal("40.50"))).isEqualTo("肆拾元伍角");
        assertThat(VoucherAmountWords.of(new BigDecimal("40.56"))).isEqualTo("肆拾元伍角陆分");
        assertThat(VoucherAmountWords.of(new BigDecimal("1005.00"))).isEqualTo("壹仟零伍元整");
        assertThat(VoucherAmountWords.of(new BigDecimal("10000.00"))).isEqualTo("壹万元整");
        assertThat(VoucherAmountWords.of(new BigDecimal("100000000.00"))).isEqualTo("壹亿元整");
        assertThat(VoucherAmountWords.of(new BigDecimal("-40.00"))).isEqualTo("负肆拾元整");
        assertThat(VoucherAmountWords.of(BigDecimal.ZERO)).isEqualTo("零元整");
    }
}
