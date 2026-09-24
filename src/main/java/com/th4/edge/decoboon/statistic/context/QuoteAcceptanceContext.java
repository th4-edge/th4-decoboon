package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.tenant.user.model.Employee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 报价接受率统一口径（{@code MET-001} + {@code DEC-001} 决策一）。
 *
 * <pre>
 * 最终报价接受率 = 最终为 Accepted/Converted 的报价机会数 ÷ 最终为 Accepted/Converted/Rejected 的报价机会数
 * 首版报价接受率 = 首版被 Accepted/Converted 的报价机会数 ÷ 首版已产生 Accepted/Rejected/Converted 的报价机会数
 * 报价机会 = 客户 + 房屋；草稿、待决策、已被替代不进正式分母；待决策报价量单独展示。
 * </pre>
 *
 * <p>门店、团队、设计师、人员绩效和经营分析必须共用本口径，禁止各自使用
 * {@code Accepted / 全部报价} 之类的分母。</p>
 *
 * <p>本类只做纯计算，报价的懒加载关联（客户、房屋、负责人）由调用方在事务边界内加载，
 * 因此仅应从事务内的 Provider/Context 调用。</p>
 */
public final class QuoteAcceptanceContext {

    private static final Set<QuoteStatus> DECIDED =
            Set.of(QuoteStatus.Accepted, QuoteStatus.Rejected, QuoteStatus.Converted);
    private static final Set<QuoteStatus> ACCEPTED =
            Set.of(QuoteStatus.Accepted, QuoteStatus.Converted);
    private static final Set<QuoteStatus> NOT_OPPORTUNITY =
            Set.of(QuoteStatus.Draft, QuoteStatus.Superseded);

    private QuoteAcceptanceContext() {
    }

    /** 单条报价机会（客户 + 房屋）的最终客户决策。 */
    public record Opportunity(String key,
                              Quote firstQuote,
                              Quote decidingQuote,
                              boolean decided,
                              boolean accepted) {

        /** 首版是否已经产生接受或拒绝结果。 */
        public boolean firstDecided() {
            return firstQuote != null && firstQuote.getStatus() != null
                    && DECIDED.contains(firstQuote.getStatus());
        }

        /** 首版是否被接受。 */
        public boolean firstAccepted() {
            return firstDecided() && ACCEPTED.contains(firstQuote.getStatus());
        }

        /**
         * 机会口径下的决策时间；历史报价缺少 {@code acceptedAt/rejectedAt/convertedAt}
         * 时回退到报价创建时间，用于周期归属（兼容策略见 EXE-005）。
         */
        public LocalDateTime decidedAt() {
            if (decidingQuote == null) {
                return firstQuote == null || firstQuote.getCreateTime() == null
                        ? null : firstQuote.getCreateTime().toLocalDateTime();
            }
            LocalDateTime decided = decisionAt(decidingQuote);
            if (decided != null) {
                return decided;
            }
            return decidingQuote.getCreateTime() == null ? null : decidingQuote.getCreateTime().toLocalDateTime();
        }

        /** 机会归属人：有决策时归属最终决策报价负责人，未决策时归属首版报价负责人。 */
        public Employee owner() {
            Quote quote = decidingQuote != null ? decidingQuote : firstQuote;
            return quote == null ? null : quote.getCreatedBy();
        }

        public boolean ownedBy(Employee employee) {
            return sameEmployee(employee, owner());
        }
    }

    /** 报价接受率统计结果。 */
    public record Result(int submittedQuoteCount,
                         int pendingDecisionQuoteCount,
                         int decidedOpportunityCount,
                         int acceptedOpportunityCount,
                         int firstDecidedOpportunityCount,
                         int firstAcceptedOpportunityCount,
                         BigDecimal finalAcceptanceRate,
                         BigDecimal firstAcceptanceRate) {

        public static Result empty() {
            return new Result(0, 0, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    /** 报价范围（全部报价）的整体口径统计。 */
    public static Result analyze(Collection<Quote> quotes) {
        return summarize(opportunities(quotes), quotes);
    }

    /** 指定归属人的口径统计：机会按最终决策报价负责人归属。 */
    public static Result analyze(Collection<Quote> quotes, Employee owner) {
        List<Opportunity> owned = opportunities(quotes).stream()
                .filter(opportunity -> opportunity.ownedBy(owner))
                .toList();
        List<Quote> ownerQuotes = quotes == null ? List.of() : quotes.stream()
                .filter(quote -> quote != null && sameEmployee(owner, quote.getCreatedBy()))
                .toList();
        return summarize(owned, ownerQuotes);
    }

    /** 按客户 + 房屋聚合报价机会，排除草稿与已被替代报价。 */
    public static List<Opportunity> opportunities(Collection<Quote> quotes) {
        Map<String, List<Quote>> groups = new LinkedHashMap<>();
        if (quotes != null) {
            for (Quote quote : quotes) {
                if (quote == null || quote.getStatus() == null || NOT_OPPORTUNITY.contains(quote.getStatus())) {
                    continue;
                }
                groups.computeIfAbsent(opportunityKey(quote), key -> new ArrayList<>()).add(quote);
            }
        }
        List<Opportunity> opportunities = new ArrayList<>(groups.size());
        for (Map.Entry<String, List<Quote>> entry : groups.entrySet()) {
            List<Quote> group = entry.getValue();
            List<Quote> decidedQuotes = group.stream()
                    .filter(item -> DECIDED.contains(item.getStatus()))
                    .sorted(Comparator.comparing(QuoteAcceptanceContext::decisionAt,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            Quote decidingQuote = decidedQuotes.isEmpty() ? null : decidedQuotes.get(decidedQuotes.size() - 1);
            Quote firstQuote = group.stream()
                    .min(Comparator.comparing(Quote::getVersion, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            opportunities.add(new Opportunity(entry.getKey(), firstQuote, decidingQuote,
                    decidingQuote != null,
                    decidingQuote != null && ACCEPTED.contains(decidingQuote.getStatus())));
        }
        return List.copyOf(opportunities);
    }

    /** 汇总机会集合与当期报价量：待决策报价量单独返回，不进入分母。 */
    public static Result summarize(Collection<Opportunity> opportunities, Collection<Quote> periodQuotes) {
        int submitted = 0;
        int pending = 0;
        if (periodQuotes != null) {
            for (Quote quote : periodQuotes) {
                if (quote == null || quote.getStatus() == null) {
                    continue;
                }
                if (!NOT_OPPORTUNITY.contains(quote.getStatus())) {
                    submitted++;
                }
                if (quote.getStatus() == QuoteStatus.Submitted) {
                    pending++;
                }
            }
        }
        int decided = 0;
        int accepted = 0;
        int firstDecided = 0;
        int firstAccepted = 0;
        if (opportunities != null) {
            for (Opportunity opportunity : opportunities) {
                if (opportunity.decided()) {
                    decided++;
                    if (opportunity.accepted()) {
                        accepted++;
                    }
                }
                if (opportunity.firstDecided()) {
                    firstDecided++;
                    if (opportunity.firstAccepted()) {
                        firstAccepted++;
                    }
                }
            }
        }
        return new Result(submitted, pending, decided, accepted, firstDecided, firstAccepted,
                rate(accepted, decided), rate(firstAccepted, firstDecided));
    }

    private static String opportunityKey(Quote quote) {
        Long custId = quote.getCustomer() == null ? null : quote.getCustomer().getCustId();
        Long propertyId = quote.getProperty() == null ? null : quote.getProperty().getPropertyId();
        return (custId == null ? "unknown" : custId) + "#" + (propertyId == null ? "none" : propertyId);
    }

    private static LocalDateTime decisionAt(Quote quote) {
        if (quote.getAcceptedAt() != null) {
            return quote.getAcceptedAt();
        }
        if (quote.getRejectedAt() != null) {
            return quote.getRejectedAt();
        }
        return quote.getConvertedAt();
    }

    private static boolean sameEmployee(Employee left, Employee right) {
        return left != null && right != null && left.getEmpId() != null
                && left.getEmpId().equals(right.getEmpId());
    }

    private static BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
