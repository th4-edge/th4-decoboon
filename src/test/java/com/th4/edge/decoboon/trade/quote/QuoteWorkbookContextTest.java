package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.decoboon.quote.context.QuoteFormulaContext;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.quote.context.QuoteWorkbookContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报价工作表批量保存的价格校验（ANS-BIZ-20261002-002 Q4）。
 *
 * 覆盖：0 价、负价、四类计价模式、未传字段保持原值、显式有效价更新、校验失败不落库。
 */
class QuoteWorkbookContextTest {

    private final QuoteLineRepository lineRepository = mock(QuoteLineRepository.class);
    private final QuoteCalcContext calcContext = new QuoteCalcContext();
    private final QuoteFormulaContext formulaContext = new QuoteFormulaContext();
    private final QuoteAdjustmentContext adjustmentContext = mock(QuoteAdjustmentContext.class);
    private final QuoteViewContext viewContext = mock(QuoteViewContext.class);
    private final QuoteWorkbookContext context = new QuoteWorkbookContext(
            lineRepository, calcContext, formulaContext, adjustmentContext, viewContext);

    // ───────────────────────── 负价 ─────────────────────────

    @Test
    void shouldRejectNegativeMaterialPrice() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        assertRejected(line, change(line, c -> c.setMaterialPrice(new BigDecimal("-1"))), "材料单价不能为负");
    }

    @Test
    void shouldRejectNegativeManualPrice() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        assertRejected(line, change(line, c -> c.setManualPrice(new BigDecimal("-1"))), "人工单价不能为负");
    }

    @Test
    void shouldRejectNegativeFixedTotalPrice() {
        QuoteLine line = line(PricingMethod.FixedTotal, "0", "0");
        line.setFixedTotalPrice(new BigDecimal("100.00"));
        assertRejected(line, change(line, c -> c.setFixedTotalPrice(new BigDecimal("-1"))), "固定总价不能为负");
    }

    // ───────────────────────── 零价（按计价模式）─────────────────────────

    /**
     * 冻结口径：materialPrice 传 0 视为「未修改」（实体默认 0，无法与未传区分），
     * 因此校验的是**有效价格**——当前材料价本就是 0 的 MaterialOnly 行必须被阻断。
     */
    @Test
    void shouldRejectMaterialOnlyLineWhoseEffectiveMaterialPriceIsZero() {
        QuoteLine line = line(PricingMethod.MaterialOnly, "0", "0");
        assertRejected(line, change(line, c -> c.setQuantity(new BigDecimal("3"))), "仅材料计价要求材料单价大于零");
    }

    /** 冻结口径：materialPrice 传 0 视为未修改，保持原值（不是静默丢弃有效修改）。 */
    @Test
    void shouldTreatZeroMaterialPriceAsUnchanged() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        Quote request = requestWith(line, c -> c.setMaterialPrice(BigDecimal.ZERO));
        Quote quote = quoteOf(line);
        stubPersist(quote, line);

        context.update(quote, request);

        assertThat(line.getMaterialPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldRejectManualOnlyLineWhoseEffectiveManualPriceIsZero() {
        QuoteLine line = line(PricingMethod.ManualOnly, "0", "0");
        assertRejected(line, change(line, c -> c.setQuantity(new BigDecimal("3"))), "仅人工计价要求人工单价大于零");
    }

    @Test
    void shouldRejectMaterialAndManualLineWithZeroEffectiveTotal() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "0", "0");
        assertRejected(line, change(line, c -> c.setQuantity(new BigDecimal("3"))), "材料单价与人工单价之和必须大于零");
    }

    @Test
    void shouldRejectZeroFixedTotalForFixedTotal() {
        QuoteLine line = line(PricingMethod.FixedTotal, "0", "0");
        line.setFixedTotalPrice(new BigDecimal("100.00"));
        assertRejected(line, change(line, c -> c.setFixedTotalPrice(BigDecimal.ZERO)), "固定总价计价要求固定总价大于零");
    }

    // ───────────────────────── 单侧为 0 允许 ─────────────────────────

    @Test
    void shouldAllowZeroManualPriceForMaterialOnly() {
        QuoteLine line = line(PricingMethod.MaterialOnly, "10.00", "0");
        Quote request = requestWith(line, change -> {
            change.setMaterialPrice(new BigDecimal("12.00"));
            change.setManualPrice(BigDecimal.ZERO);
        });
        Quote quote = quoteOf(line);
        stubPersist(quote, line);

        context.update(quote, request);

        assertThat(line.getMaterialPrice()).isEqualByComparingTo("12.00");
        assertThat(line.getManualPrice()).isEqualByComparingTo("0");
    }

    @Test
    void shouldAllowZeroMaterialPriceForManualOnly() {
        QuoteLine line = line(PricingMethod.ManualOnly, "0", "20.00");
        Quote request = requestWith(line, change -> {
            change.setManualPrice(new BigDecimal("22.00"));
            change.setMaterialPrice(BigDecimal.ZERO);
        });
        Quote quote = quoteOf(line);
        stubPersist(quote, line);

        context.update(quote, request);

        assertThat(line.getManualPrice()).isEqualByComparingTo("22.00");
        assertThat(line.getMaterialPrice()).isEqualByComparingTo("0");
    }

    // ───────────────────────── 不适用字段必须显式报错 ─────────────────────────

    @Test
    void shouldRejectManualPriceForMaterialOnlyWhenNonZero() {
        QuoteLine line = line(PricingMethod.MaterialOnly, "10.00", "0");
        assertRejected(line, change(line, c -> c.setManualPrice(new BigDecimal("5"))), "仅材料计价的报价行不接受人工单价");
    }

    @Test
    void shouldRejectMaterialPriceForManualOnlyWhenNonZero() {
        QuoteLine line = line(PricingMethod.ManualOnly, "0", "20.00");
        assertRejected(line, change(line, c -> c.setMaterialPrice(new BigDecimal("5"))), "仅人工计价的报价行不接受材料单价");
    }

    @Test
    void shouldRejectPriceForFixedTotalMode() {
        QuoteLine line = line(PricingMethod.FixedTotal, "0", "0");
        line.setFixedTotalPrice(new BigDecimal("100.00"));
        assertRejected(line, change(line, c -> c.setMaterialPrice(new BigDecimal("5"))), "固定总价计价的报价行不接受材料单价或人工单价");
    }

    @Test
    void shouldRejectFixedTotalForNonFixedTotalMode() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        assertRejected(line, change(line, c -> c.setFixedTotalPrice(new BigDecimal("100"))), "非固定总价计价的报价行不接受固定总价");
    }

    // ───────────────────────── 未传保持原值 / 显式有效价更新 ─────────────────────────

    @Test
    void shouldKeepExistingPricesWhenFieldsOmitted() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        Quote request = requestWith(line, change -> change.setQuantity(new BigDecimal("3")));
        Quote quote = quoteOf(line);
        stubPersist(quote, line);

        context.update(quote, request);

        assertThat(line.getMaterialPrice()).isEqualByComparingTo("10.00");
        assertThat(line.getManualPrice()).isEqualByComparingTo("5.00");
        assertThat(line.getQuantity()).isEqualByComparingTo("3");
    }

    @Test
    void shouldApplyExplicitValidPricesAndRecalculate() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        Quote request = requestWith(line, change -> {
            change.setMaterialPrice(new BigDecimal("40.00"));
            change.setManualPrice(new BigDecimal("30.00"));
            change.setQuantity(new BigDecimal("30"));
        });
        Quote quote = quoteOf(line);
        stubPersist(quote, line);

        context.update(quote, request);

        assertThat(line.getMaterialPrice()).isEqualByComparingTo("40.00");
        assertThat(line.getManualPrice()).isEqualByComparingTo("30.00");
        assertThat(line.getPrice()).isEqualByComparingTo("70.00");
        assertThat(line.getAmount()).isEqualByComparingTo("2100.00");
    }

    // ───────────────────────── 错误不落库 ─────────────────────────

    @Test
    void shouldNotPersistAnyLineWhenValidationFails() {
        QuoteLine ok = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        QuoteLine bad = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        bad.setItemId(3L);

        QuoteLine okChange = baseChange(ok);
        okChange.setMaterialPrice(new BigDecimal("12.00"));
        QuoteLine badChange = baseChange(bad);
        badChange.setManualPrice(new BigDecimal("-1"));

        Quote quote = quoteOf(ok, bad);
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(ok, bad));

        Quote request = new Quote();
        request.setItems(List.of(okChange, badChange));

        assertThatThrownBy(() -> context.update(quote, request))
                .hasMessageContaining("人工单价不能为负");

        // 校验先于写入：任何一行都不得落库，且不得触发重算
        verify(lineRepository, never()).save(any());
        verify(adjustmentContext, never()).recalculate(any());
        assertThat(ok.getMaterialPrice()).isEqualByComparingTo("10.00");
    }

    // ───────────────────────── 辅助 ─────────────────────────

    private QuoteLine line(PricingMethod mode, String material, String manual) {
        QuoteLine line = new QuoteLine();
        line.setItemId(2L);
        line.setPricingMode(mode);
        line.setMaterialPrice(new BigDecimal(material));
        line.setManualPrice(new BigDecimal(manual));
        line.setQuantity(new BigDecimal("2"));
        line.setCost(new BigDecimal("8.00"));
        line.setAmount(new BigDecimal("30.00"));
        line.setIncluded(true);
        return line;
    }

    private Quote quoteOf(QuoteLine... lines) {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setStatus(QuoteStatus.Draft);
        return quote;
    }

    private QuoteLine baseChange(QuoteLine target) {
        QuoteLine change = new QuoteLine();
        change.setItemId(target.getItemId());
        change.setQuantity(target.getQuantity());
        return change;
    }

    private QuoteLine change(QuoteLine target, java.util.function.Consumer<QuoteLine> customizer) {
        QuoteLine change = baseChange(target);
        customizer.accept(change);
        return change;
    }

    private Quote requestWith(QuoteLine target, java.util.function.Consumer<QuoteLine> customizer) {
        Quote request = new Quote();
        request.setItems(List.of(change(target, customizer)));
        return request;
    }

    private void assertRejected(QuoteLine line, QuoteLine change, String message) {
        Quote quote = quoteOf(line);
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(line));
        Quote request = new Quote();
        request.setItems(List.of(change));

        assertThatThrownBy(() -> context.update(quote, request))
                .hasMessageContaining(message);
        verify(lineRepository, never()).save(any());
    }

    /** 统一的持久化桩：必须使用与 update() 同一个 Quote 实例，否则 findByQuote 匹配不上。 */
    private void stubPersist(Quote quote, QuoteLine line) {
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(line));
        when(lineRepository.save(line)).thenReturn(line);
        when(viewContext.apply(any(), anyList())).thenReturn(new Quote());
    }
}
