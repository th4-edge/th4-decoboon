package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteBusinessStateContext;
import com.th4.edge.decoboon.quote.model.Quote;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 报价服务端状态视图测试。 */
class QuoteBusinessStateContextTest {

    private final QuoteBusinessStateContext context = new QuoteBusinessStateContext();

    @Test
    void draftQuoteShouldReturnContinueAction() {
        Quote quote = quote(QuoteStatus.Draft);
        context.enrich(quote);
        assertThat(quote.getBusinessStateView()).isNotNull();
        assertThat(quote.getBusinessStateView().getStatus()).isEqualTo("Draft");
        assertThat(quote.getBusinessStateView().getNextAction().getKey()).isEqualTo("quote/continue");
        assertThat(quote.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Submitted");
    }

    @Test
    void submittedQuoteShouldReturnFollowDecisionAction() {
        Quote quote = quote(QuoteStatus.Submitted);
        context.enrich(quote);
        assertThat(quote.getBusinessStateView().getNextAction().getKey()).isEqualTo("quote/follow-decision");
        assertThat(quote.getBusinessStateView().getNextAction().getToStatus()).contains("Accepted");
    }

    @Test
    void acceptedQuoteShouldReturnContractHandoffAction() {
        Quote quote = quote(QuoteStatus.Accepted);
        context.enrich(quote);
        assertThat(quote.getBusinessStateView().getNextAction().getKey()).isEqualTo("contract/handoff");
        assertThat(quote.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Converted");
    }

    @Test
    void rejectedQuoteShouldReturnReviseAction() {
        Quote quote = quote(QuoteStatus.Rejected);
        context.enrich(quote);
        assertThat(quote.getBusinessStateView().getNextAction().getKey()).isEqualTo("quote/revise");
        assertThat(quote.getBusinessStateView().getNextAction().getToStatus()).isEqualTo("Draft");
    }

    @Test
    void supersededQuoteShouldDisableAction() {
        Quote quote = quote(QuoteStatus.Superseded);
        context.enrich(quote);
        assertThat(quote.getBusinessStateView().getNextAction().getKey()).isEqualTo("quote/history");
        assertThat(quote.getBusinessStateView().getNextAction().getEnabled()).isFalse();
    }

    @Test
    void missingQuoteShouldBeIgnored() {
        context.enrich(null);
        context.enrich(new Quote());
    }

    private Quote quote(QuoteStatus status) {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setStatus(status);
        quote.setVersion(2);
        quote.setCostComplete(Boolean.TRUE);
        return quote;
    }
}
