package com.th4.edge.decoboon.quote.repository;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 报价明细仓储。
 */
public interface QuoteLineRepository extends JpaRepository<QuoteLine, Long> {

    List<QuoteLine> findByQuote(Quote quote);

    boolean existsByDesignItem_ItemId(Long itemId);
}
