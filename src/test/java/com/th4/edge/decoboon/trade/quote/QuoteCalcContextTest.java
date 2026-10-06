package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.framework.exception.DefinedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteCalcContextTest {

    private final QuoteCalcContext context = new QuoteCalcContext();

    @Test
    void shouldCalculateMaterialAndManualByQuantity() {
        BigDecimal amount = context.amount(PricingMethod.MaterialAndManual, new BigDecimal("12.5"),
                new BigDecimal("80"), new BigDecimal("45"));

        assertThat(amount).isEqualByComparingTo("1562.50");
    }

    @Test
    void shouldCalculateMaterialOnlyAndManualOnly() {
        assertThat(context.amount(PricingMethod.MaterialOnly, new BigDecimal("3"),
                new BigDecimal("99"), new BigDecimal("50"))).isEqualByComparingTo("297.00");
        assertThat(context.amount(PricingMethod.ManualOnly, new BigDecimal("3"),
                new BigDecimal("99"), new BigDecimal("50"))).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldIgnoreQuantityForFixedTotal() {
        assertThat(context.amount(PricingMethod.FixedTotal, new BigDecimal("8"),
                new BigDecimal("1200"), new BigDecimal("300"))).isEqualByComparingTo("1500.00");
    }

    @Test
    void shouldWriteBackAmountCostAndUnitPrice() {
        DesignItem item = new DesignItem();
        item.setQuantity(new BigDecimal("4"));
        item.setMaterialPrice(new BigDecimal("30"));
        item.setManualPrice(new BigDecimal("20"));
        item.setCostPrice(new BigDecimal("25"));
        item.setPricingMode(PricingMethod.MaterialAndManual);

        context.calculate(item);

        assertThat(item.getAmount()).isEqualByComparingTo("200.00");
        assertThat(item.getCost()).isEqualByComparingTo("100.00");
        assertThat(context.unitPrice(item.getPricingMode(), item.getQuantity(),
                item.getMaterialPrice(), item.getManualPrice())).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldAllowZeroQuantityAsGiftAndReturnZeroAmount() {
        // 业务确认（2026-10-06）：设计分项允许数量 0 赠送，金额与成本为 0，单价仍取项目库基价。
        assertThat(context.amount(PricingMethod.MaterialAndManual, BigDecimal.ZERO,
                new BigDecimal("80"), new BigDecimal("45"))).isEqualByComparingTo("0.00");
        assertThat(context.cost(BigDecimal.ZERO, new BigDecimal("25"))).isEqualByComparingTo("0.00");
        assertThat(context.unitPrice(PricingMethod.MaterialAndManual, BigDecimal.ZERO,
                new BigDecimal("80"), new BigDecimal("45"))).isEqualByComparingTo("125.00");

        DesignItem gift = new DesignItem();
        gift.setQuantity(BigDecimal.ZERO);
        gift.setMaterialPrice(new BigDecimal("80"));
        gift.setManualPrice(new BigDecimal("45"));
        gift.setCostPrice(new BigDecimal("25"));
        gift.setPricingMode(PricingMethod.MaterialAndManual);

        context.calculate(gift);

        assertThat(gift.getAmount()).isEqualByComparingTo("0.00");
        assertThat(gift.getCost()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> context.amount(PricingMethod.MaterialOnly, new BigDecimal("-1"),
                BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不能为负数");
    }
}
