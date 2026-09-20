package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.context.QuoteCalcContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuoteAdjustmentContextTest {

    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final QuoteLineRepository lineRepository = mock(QuoteLineRepository.class);
    private final CatalogItemRepository catalogRepository = mock(CatalogItemRepository.class);
    private final QuoteCalcContext calcContext = new QuoteCalcContext();
    private final QuoteAdjustmentContext context = new QuoteAdjustmentContext(
            quoteRepository, lineRepository, catalogRepository, calcContext);

    @Test
    void shouldDeductBasePackageLineAndRecalculateQuote() {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setStatus(QuoteStatus.Draft);
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxRate(new BigDecimal("0.05"));
        PackageLine packageLine = new PackageLine();
        packageLine.setDeductible(true);
        QuoteLine base = new QuoteLine();
        base.setItemId(2L);
        base.setPackageLine(packageLine);
        base.setIncluded(true);
        base.setAmount(new BigDecimal("1000.00"));
        base.setCost(new BigDecimal("600.00"));
        when(lineRepository.findByQuote(quote)).thenReturn(List.of(base));
        when(lineRepository.save(base)).thenReturn(base);

        QuoteLine result = context.save(quote, new QuoteLine(), true, 2L, null, null);

        assertThat(result.getIncluded()).isFalse();
        assertThat(result.getAmount()).isEqualByComparingTo("0");
        assertThat(result.getCost()).isEqualByComparingTo("0");
        assertThat(quote.getSubtotal()).isEqualByComparingTo("0");
        assertThat(quote.getAmount()).isEqualByComparingTo("0");
        assertThat(quote.getCost()).isEqualByComparingTo("0");
        verify(quoteRepository).save(quote);
    }
}
