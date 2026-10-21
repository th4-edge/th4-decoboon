package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 设计方案当前价格投影。
 *
 * <p>价格口径由 DEC-078 冻结：正式报价优先；没有正式报价时由前端展示方案计算价。
 * 本上下文只负责统一服务端事实，不改变设计或报价状态。</p>
 */
@Note("设计方案当前价格投影")
@Component
public class DesignPriceContext {

    private final DesignItemRepository itemRepository;
    private final SpaceRepository spaceRepository;
    private final QuoteRepository quoteRepository;

    public DesignPriceContext(DesignItemRepository itemRepository,
                              SpaceRepository spaceRepository,
                              QuoteRepository quoteRepository) {
        this.itemRepository = itemRepository;
        this.spaceRepository = spaceRepository;
        this.quoteRepository = quoteRepository;
    }

    /** 批量补齐列表价格，查询次数固定，不随方案数增长。 */
    @Transactional(readOnly = true)
    public void enrich(Collection<Design> designs) {
        if (designs == null || designs.isEmpty()) return;
        List<Long> designIds = designs.stream()
                .map(Design::getDesignId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (designIds.isEmpty()) return;

        Map<Long, AmountSummary> amounts = new HashMap<>();
        for (Object[] row : itemRepository.summarizeByDesignIds(designIds)) {
            Long designId = (Long) row[0];
            BigDecimal amount = row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString());
            Long count = row[2] == null ? 0L : ((Number) row[2]).longValue();
            amounts.put(designId, new AmountSummary(amount, count.intValue()));
        }

        Map<Long, Integer> spaceCounts = new HashMap<>();
        for (Object[] row : spaceRepository.countByDesignIds(designIds)) {
            spaceCounts.put((Long) row[0], ((Number) row[1]).intValue());
        }

        Map<Long, Quote> currentQuotes = new HashMap<>();
        for (Quote quote : quoteRepository.findByDesign_DesignIdInOrderByVersionDesc(designIds)) {
            if (quote.getDesign() == null || quote.getDesign().getDesignId() == null) continue;
            Long designId = quote.getDesign().getDesignId();
            Quote existing = currentQuotes.get(designId);
            if (isBetterCurrent(quote, existing)) currentQuotes.put(designId, quote);
        }

        for (Design design : designs) {
            AmountSummary summary = amounts.getOrDefault(design.getDesignId(), AmountSummary.EMPTY);
            design.setCalculatedAmount(summary.amount());
            design.setItemCount(summary.itemCount());
            design.setSpaceCount(spaceCounts.getOrDefault(design.getDesignId(), 0));
            applyQuote(design, currentQuotes.get(design.getDesignId()));
        }
    }

    /** 详情已有分项事实时直接汇总，避免重复读取分项明细。 */
    @Transactional(readOnly = true)
    public void enrich(Design design, List<DesignItem> items, int spaceCount) {
        if (design == null) return;
        BigDecimal amount = items == null ? BigDecimal.ZERO : items.stream()
                .map(DesignItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        design.setCalculatedAmount(amount);
        design.setItemCount(items == null ? 0 : items.size());
        design.setSpaceCount(spaceCount);
        Quote current = currentQuote(quoteRepository.findByDesign_DesignIdOrderByVersionDesc(design.getDesignId()));
        applyQuote(design, current);
    }

    public Quote currentQuote(List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) return null;
        Quote current = null;
        for (Quote quote : quotes) {
            if (isBetterCurrent(quote, current)) current = quote;
        }
        return current;
    }

    private boolean isBetterCurrent(Quote candidate, Quote current) {
        if (candidate == null || candidate.getStatus() == QuoteStatus.Superseded) return false;
        if (current == null) return true;
        int candidateRank = statusRank(candidate.getStatus());
        int currentRank = statusRank(current.getStatus());
        if (candidateRank != currentRank) return candidateRank < currentRank;
        Long candidateId = candidate.getQuoteId();
        Long currentId = current.getQuoteId();
        return candidateId != null && (currentId == null || candidateId > currentId);
    }

    /** Converted > Accepted > 其他未替代状态；同组内取最新报价索引。 */
    private int statusRank(QuoteStatus status) {
        if (status == QuoteStatus.Converted) return 0;
        if (status == QuoteStatus.Accepted) return 1;
        return 2;
    }

    private void applyQuote(Design design, Quote quote) {
        if (quote == null) {
            design.setCurrentQuoteId(null);
            design.setCurrentQuoteNo(null);
            design.setCurrentQuoteVersion(null);
            design.setCurrentQuoteStatus(null);
            design.setCurrentQuoteStatusName(null);
            design.setCurrentQuoteAmount(null);
            return;
        }
        design.setCurrentQuoteId(quote.getQuoteId());
        design.setCurrentQuoteNo(quote.getNo());
        design.setCurrentQuoteVersion(quote.getVersion());
        design.setCurrentQuoteStatus(quote.getStatus());
        design.setCurrentQuoteStatusName(quote.getStatus() == null ? null : quote.getStatus().getName());
        design.setCurrentQuoteAmount(quote.getAmount());
    }

    private record AmountSummary(BigDecimal amount, int itemCount) {
        private static final AmountSummary EMPTY = new AmountSummary(BigDecimal.ZERO, 0);
    }
}
