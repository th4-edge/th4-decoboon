package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.context.DesignPriceContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesignPriceContextTest {

    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final DesignPriceContext context =
            new DesignPriceContext(itemRepository, spaceRepository, quoteRepository);

    @Test
    void currentQuoteMustPreferConvertedThenAcceptedThenLatestOtherwise() {
        Design design = design(10L);
        Quote converted = quote(101L, design, QuoteStatus.Converted, 3);
        Quote accepted = quote(102L, design, QuoteStatus.Accepted, 4);
        Quote draft = quote(103L, design, QuoteStatus.Draft, 5);
        Quote superseded = quote(104L, design, QuoteStatus.Superseded, 6);

        when(itemRepository.summarizeByDesignIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, new BigDecimal("12860.00"), 8L}));
        when(spaceRepository.countByDesignIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 3L}));
        when(quoteRepository.findByDesign_DesignIdInOrderByVersionDesc(anyCollection()))
                .thenReturn(List.of(superseded, draft, accepted, converted));

        context.enrich(List.of(design));

        assertThat(design.getCalculatedAmount()).isEqualByComparingTo("12860.00");
        assertThat(design.getItemCount()).isEqualTo(8);
        assertThat(design.getSpaceCount()).isEqualTo(3);
        assertThat(design.getCurrentQuoteId()).isEqualTo(101L);
        assertThat(design.getCurrentQuoteStatus()).isEqualTo(QuoteStatus.Converted);
        assertThat(design.getCurrentQuoteStatusName()).isEqualTo("已转项目");
    }

    @Test
    void withoutAcceptedQuoteUseLatestNonSupersededQuote() {
        Design design = design(20L);
        Quote rejected = quote(201L, design, QuoteStatus.Rejected, 2);
        Quote submitted = quote(202L, design, QuoteStatus.Submitted, 3);
        Quote superseded = quote(203L, design, QuoteStatus.Superseded, 4);

        when(itemRepository.summarizeByDesignIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{20L, new BigDecimal("500.00"), 2L}));
        when(spaceRepository.countByDesignIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{20L, 1L}));
        when(quoteRepository.findByDesign_DesignIdInOrderByVersionDesc(anyCollection()))
                .thenReturn(List.of(rejected, submitted, superseded));

        context.enrich(List.of(design));

        assertThat(design.getCurrentQuoteId()).isEqualTo(202L);
        assertThat(design.getCurrentQuoteStatus()).isEqualTo(QuoteStatus.Submitted);
    }

    private Design design(Long id) {
        Design design = new Design();
        design.setDesignId(id);
        return design;
    }

    private Quote quote(Long id, Design design, QuoteStatus status, int version) {
        Quote quote = new Quote();
        quote.setQuoteId(id);
        quote.setDesign(design);
        quote.setStatus(status);
        quote.setVersion(version);
        quote.setNo("Q-" + id);
        quote.setAmount(new BigDecimal("1000.00"));
        return quote;
    }
}
