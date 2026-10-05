package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.decoboon.quote.context.QuoteFormulaContext;
import com.th4.edge.decoboon.quote.context.QuoteRevisionContext;
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
 * 报价工作表批量保存的「缺省即保留」与价格校验。
 *
 * ECD-014：`QuoteLine.quantity` 默认值是 ONE，嵌套绑定新对象时「未传」与「显式传 1」无法区分，
 * 导致"只改价格"的请求把工程量覆盖成 1。修复方式为请求携带 `*Provided` 显式标记，
 * 未标记 = 未提供 = 保留原值（标记为 @Transient，不持久化、不序列化）。
 */
class QuoteWorkbookContextTest {

    private final QuoteLineRepository lineRepository = mock(QuoteLineRepository.class);
    private final QuoteCalcContext calcContext = new QuoteCalcContext();
    private final QuoteFormulaContext formulaContext = new QuoteFormulaContext();
    private final QuoteAdjustmentContext adjustmentContext = mock(QuoteAdjustmentContext.class);
    private final QuoteViewContext viewContext = mock(QuoteViewContext.class);
    private final QuoteWorkbookContext context = new QuoteWorkbookContext(
            lineRepository, calcContext, formulaContext, adjustmentContext, viewContext,
            new QuoteRevisionContext());

    // ───────────────── ECD-014 核心复现：只改价格不得覆盖工程量 ─────────────────

    @Test
    void shouldKeepQuantityWhenOnlyMaterialPriceProvided() {
        QuoteLine line = line(PricingMethod.MaterialOnly, "10.00", "0");
        line.setQuantity(new BigDecimal("30"));

        QuoteLine change = changeOf(line);
        change.setMaterialPrice(new BigDecimal("12.00"));
        change.setMaterialPriceProvided(true);

        QuoteLine saved = save(line, change);

        assertThat(saved.getQuantity()).isEqualByComparingTo("30");   // ← 修复前会变成 1
        assertThat(saved.getMaterialPrice()).isEqualByComparingTo("12.00");
        assertThat(saved.getAmount()).isEqualByComparingTo("360.00"); // 12 × 30
    }

    @Test
    void shouldKeepQuantityWhenOnlyManualPriceProvided() {
        QuoteLine line = line(PricingMethod.ManualOnly, "0", "20.00");
        line.setQuantity(new BigDecimal("12"));

        QuoteLine change = changeOf(line);
        change.setManualPrice(new BigDecimal("25.00"));
        change.setManualPriceProvided(true);

        QuoteLine saved = save(line, change);

        assertThat(saved.getQuantity()).isEqualByComparingTo("12");
        assertThat(saved.getManualPrice()).isEqualByComparingTo("25.00");
        assertThat(saved.getAmount()).isEqualByComparingTo("300.00"); // 25 × 12
    }

    // ───────────────── 三路取值：公式 > 显式 quantity > 保留原值 ─────────────────

    @Test
    void shouldKeepQuantityWhenNeitherQuantityNorFormulaProvided() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        line.setQuantity(new BigDecimal("7"));

        QuoteLine change = changeOf(line);          // 什么标记都不带
        QuoteLine saved = save(line, change);

        assertThat(saved.getQuantity()).isEqualByComparingTo("7");
    }

    @Test
    void shouldUseExplicitQuantityWhenProvided() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        line.setQuantity(new BigDecimal("7"));

        QuoteLine change = changeOf(line);
        change.setQuantity(new BigDecimal("9"));
        change.setQuantityProvided(true);

        QuoteLine saved = save(line, change);

        assertThat(saved.getQuantity()).isEqualByComparingTo("9");
        assertThat(saved.getAmount()).isEqualByComparingTo("135.00"); // 15 × 9
    }

    @Test
    void shouldUseFormulaResultWhenFormulaProvided() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        line.setQuantity(new BigDecimal("7"));

        QuoteLine change = changeOf(line);
        change.setQuantityFormula("=3*4");
        change.setQuantityProvided(true);
        change.setQuantity(new BigDecimal("999"));   // 有公式时忽略显式数量

        QuoteLine saved = save(line, change);

        assertThat(saved.getQuantity()).isEqualByComparingTo("12");   // 3*4，公式优先
        assertThat(saved.getQuantityFormula()).isEqualTo("=3*4");
    }

    // ───────────────── 缺省即保留（价格） ─────────────────

    @Test
    void shouldKeepExistingPricesWhenNotProvided() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");

        QuoteLine change = changeOf(line);
        change.setQuantity(new BigDecimal("3"));
        change.setQuantityProvided(true);

        QuoteLine saved = save(line, change);

        assertThat(saved.getMaterialPrice()).isEqualByComparingTo("10.00");
        assertThat(saved.getManualPrice()).isEqualByComparingTo("5.00");
    }

    @Test
    void shouldApplyExplicitValidPrices() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");

        QuoteLine change = changeOf(line);
        change.setQuantity(new BigDecimal("30"));
        change.setQuantityProvided(true);
        change.setMaterialPrice(new BigDecimal("40.00"));
        change.setMaterialPriceProvided(true);
        change.setManualPrice(new BigDecimal("30.00"));
        change.setManualPriceProvided(true);

        QuoteLine saved = save(line, change);

        assertThat(saved.getMaterialPrice()).isEqualByComparingTo("40.00");
        assertThat(saved.getManualPrice()).isEqualByComparingTo("30.00");
        assertThat(saved.getAmount()).isEqualByComparingTo("2100.00"); // 70 × 30
    }

    // ───────────────── 显式 0 / 负价 ─────────────────

    @Test
    void shouldRejectNegativeMaterialPrice() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        QuoteLine change = changeOf(line);
        change.setMaterialPrice(new BigDecimal("-1"));
        change.setMaterialPriceProvided(true);

        assertRejected(line, change, "材料单价不能为负");
    }

    @Test
    void shouldRejectNegativeManualPrice() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        QuoteLine change = changeOf(line);
        change.setManualPrice(new BigDecimal("-1"));
        change.setManualPriceProvided(true);

        assertRejected(line, change, "人工单价不能为负");
    }

    @Test
    void shouldRejectExplicitZeroMaterialPriceForMaterialOnly() {
        QuoteLine line = line(PricingMethod.MaterialOnly, "10.00", "0");
        QuoteLine change = changeOf(line);
        change.setMaterialPrice(BigDecimal.ZERO);      // 显式 0（此前会被静默忽略）
        change.setMaterialPriceProvided(true);

        assertRejected(line, change, "仅材料计价要求材料单价大于零");
    }

    @Test
    void shouldRejectExplicitZeroManualPriceForManualOnly() {
        QuoteLine line = line(PricingMethod.ManualOnly, "0", "20.00");
        QuoteLine change = changeOf(line);
        change.setManualPrice(BigDecimal.ZERO);
        change.setManualPriceProvided(true);

        assertRejected(line, change, "仅人工计价要求人工单价大于零");
    }

    @Test
    void shouldRejectExplicitZeroFixedTotalForFixedTotal() {
        QuoteLine line = line(PricingMethod.FixedTotal, "0", "0");
        line.setFixedTotalPrice(new BigDecimal("100.00"));
        QuoteLine change = changeOf(line);
        change.setFixedTotalPrice(BigDecimal.ZERO);
        change.setFixedTotalPriceProvided(true);

        assertRejected(line, change, "固定总价计价要求固定总价大于零");
    }

    @Test
    void shouldRejectManualPriceForMaterialOnlyWhenProvided() {
        QuoteLine line = line(PricingMethod.MaterialOnly, "10.00", "0");
        QuoteLine change = changeOf(line);
        change.setManualPrice(new BigDecimal("5"));
        change.setManualPriceProvided(true);

        assertRejected(line, change, "仅材料计价的报价行不接受人工单价");
    }

    @Test
    void shouldRejectMaterialPriceForManualOnlyWhenProvided() {
        QuoteLine line = line(PricingMethod.ManualOnly, "0", "20.00");
        QuoteLine change = changeOf(line);
        change.setMaterialPrice(new BigDecimal("5"));
        change.setMaterialPriceProvided(true);

        assertRejected(line, change, "仅人工计价的报价行不接受材料单价");
    }

    @Test
    void shouldRejectFixedTotalForNonFixedTotalMode() {
        QuoteLine line = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        QuoteLine change = changeOf(line);
        change.setFixedTotalPrice(new BigDecimal("100"));
        change.setFixedTotalPriceProvided(true);

        assertRejected(line, change, "非固定总价计价的报价行不接受固定总价");
    }

    // ───────────────── 错误不落库 ─────────────────

    @Test
    void shouldNotPersistAnyLineWhenValidationFails() {
        QuoteLine ok = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        QuoteLine bad = line(PricingMethod.MaterialAndManual, "10.00", "5.00");
        bad.setItemId(3L);

        QuoteLine okChange = changeOf(ok);
        okChange.setMaterialPrice(new BigDecimal("12.00"));
        okChange.setMaterialPriceProvided(true);

        QuoteLine badChange = changeOf(bad);
        badChange.setManualPrice(new BigDecimal("-1"));
        badChange.setManualPriceProvided(true);

        Quote quote = quoteOf(ok, bad);
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(ok, bad));
        Quote request = new Quote();
        request.setItems(List.of(okChange, badChange));

        assertThatThrownBy(() -> context.update(quote, request))
                .hasMessageContaining("人工单价不能为负");

        verify(lineRepository, never()).save(any());
        verify(adjustmentContext, never()).recalculate(any());
        assertThat(ok.getMaterialPrice()).isEqualByComparingTo("10.00");
    }

    // ───────────────── 辅助 ─────────────────

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

    /** 变更对象：默认不带任何 Provided 标记（等价于"未传"）。 */
    private QuoteLine changeOf(QuoteLine target) {
        QuoteLine change = new QuoteLine();
        change.setItemId(target.getItemId());
        return change;
    }

    /** 执行一次批量保存并返回被保存的行。 */
    private QuoteLine save(QuoteLine line, QuoteLine change) {
        Quote quote = quoteOf(line);
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(line));
        when(lineRepository.save(line)).thenReturn(line);
        when(viewContext.apply(any(), anyList())).thenReturn(new Quote());
        Quote request = new Quote();
        request.setItems(List.of(change));
        context.update(quote, request);
        return line;
    }

    private void assertRejected(QuoteLine line, QuoteLine change, String message) {
        Quote quote = quoteOf(line);
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(line));
        Quote request = new Quote();
        request.setItems(List.of(change));

        assertThatThrownBy(() -> context.update(quote, request)).hasMessageContaining(message);
        verify(lineRepository, never()).save(any());
    }
}
