package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.statistic.context.QuoteAcceptanceContext;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 报价接受率统一口径回归（MET-001 + DEC-001 决策一）。
 *
 * <p>覆盖：草稿/待决策不进分母、多改版不重复计算机会、同客户多房屋互不污染、
 * 已被替代不作为拒绝、首版口径独立，以及设计师归属。</p>
 */
class QuoteAcceptanceContextTest {

    private static final LocalDateTime JULY = LocalDateTime.of(2026, 7, 10, 10, 0);
    private static final LocalDateTime AUGUST = LocalDateTime.of(2026, 8, 10, 10, 0);

    @Test
    void shouldFollowMet001Example() {
        List<Quote> quotes = new ArrayList<>();
        long id = 0;
        for (int i = 0; i < 2; i++) quotes.add(quote(++id, 100 + i, 200 + i, 1, QuoteStatus.Draft, null));
        for (int i = 0; i < 2; i++) quotes.add(quote(++id, 110 + i, 210 + i, 1, QuoteStatus.Submitted, null));
        for (int i = 0; i < 4; i++) quotes.add(quote(++id, 120 + i, 220 + i, 1, QuoteStatus.Accepted, AUGUST));
        for (int i = 0; i < 2; i++) quotes.add(quote(++id, 130 + i, 230 + i, 1, QuoteStatus.Rejected, AUGUST));

        QuoteAcceptanceContext.Result result = QuoteAcceptanceContext.analyze(quotes);

        assertThat(result.submittedQuoteCount()).isEqualTo(8);
        assertThat(result.pendingDecisionQuoteCount()).isEqualTo(2);
        assertThat(result.decidedOpportunityCount()).isEqualTo(6);
        assertThat(result.acceptedOpportunityCount()).isEqualTo(4);
        assertThat(result.finalAcceptanceRate()).isEqualByComparingTo("66.7");
    }

    @Test
    void shouldCountOneOpportunityForMultipleVersions() {
        List<Quote> quotes = List.of(
                quote(1L, 100L, 200L, 1, QuoteStatus.Rejected, JULY),
                quote(2L, 100L, 200L, 2, QuoteStatus.Accepted, AUGUST));

        QuoteAcceptanceContext.Result result = QuoteAcceptanceContext.analyze(quotes);

        assertThat(result.decidedOpportunityCount()).isEqualTo(1);
        assertThat(result.acceptedOpportunityCount()).isEqualTo(1);
        assertThat(result.finalAcceptanceRate()).isEqualByComparingTo("100.0");
        assertThat(result.firstDecidedOpportunityCount()).isEqualTo(1);
        assertThat(result.firstAcceptanceRate()).isEqualByComparingTo("0.0");
    }

    @Test
    void shouldKeepPropertiesAndCustomersSeparate() {
        List<Quote> quotes = List.of(
                quote(1L, 100L, 200L, 1, QuoteStatus.Accepted, AUGUST),
                quote(2L, 100L, 201L, 1, QuoteStatus.Rejected, AUGUST),
                quote(3L, 101L, 202L, 1, QuoteStatus.Converted, AUGUST));

        QuoteAcceptanceContext.Result result = QuoteAcceptanceContext.analyze(quotes);

        assertThat(result.decidedOpportunityCount()).isEqualTo(3);
        assertThat(result.acceptedOpportunityCount()).isEqualTo(2);
        assertThat(result.finalAcceptanceRate()).isEqualByComparingTo("66.7");
    }

    @Test
    void shouldIgnoreSupersededQuotes() {
        List<Quote> quotes = List.of(
                quote(1L, 100L, 200L, 1, QuoteStatus.Superseded, null),
                quote(2L, 100L, 200L, 2, QuoteStatus.Accepted, AUGUST));

        QuoteAcceptanceContext.Result result = QuoteAcceptanceContext.analyze(quotes);

        assertThat(result.decidedOpportunityCount()).isEqualTo(1);
        assertThat(result.acceptedOpportunityCount()).isEqualTo(1);
        assertThat(result.firstAcceptanceRate()).isEqualByComparingTo("100.0");
    }

    @Test
    void shouldAttributeOpportunityToFinalDecisionDesigner() {
        Employee first = employee(11L, "设计师甲");
        Employee second = employee(12L, "设计师乙");
        Quote rejected = quote(1L, 100L, 200L, 1, QuoteStatus.Rejected, JULY);
        rejected.setCreatedBy(first);
        Quote accepted = quote(2L, 100L, 200L, 2, QuoteStatus.Accepted, AUGUST);
        accepted.setCreatedBy(second);

        QuoteAcceptanceContext.Result forSecond = QuoteAcceptanceContext.analyze(List.of(rejected, accepted), second);
        QuoteAcceptanceContext.Result forFirst = QuoteAcceptanceContext.analyze(List.of(rejected, accepted), first);

        assertThat(forSecond.decidedOpportunityCount()).isEqualTo(1);
        assertThat(forSecond.finalAcceptanceRate()).isEqualByComparingTo("100.0");
        assertThat(forFirst.decidedOpportunityCount()).isZero();
        assertThat(forFirst.finalAcceptanceRate()).isEqualByComparingTo("0");
    }

    private Quote quote(long quoteId, long customerId, long propertyId, int version,
                        QuoteStatus status, LocalDateTime decidedAt) {
        Customer customer = new Customer();
        customer.setCustId(customerId);
        Property property = new Property();
        property.setPropertyId(propertyId);
        Quote quote = new Quote();
        quote.setQuoteId(quoteId);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setVersion(version);
        quote.setStatus(status);
        switch (status) {
            case Accepted -> quote.setAcceptedAt(decidedAt);
            case Rejected -> quote.setRejectedAt(decidedAt);
            case Converted -> quote.setConvertedAt(decidedAt);
            default -> { }
        }
        return quote;
    }

    private Employee employee(Long empId, String name) {
        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setName(name);
        return employee;
    }
}
