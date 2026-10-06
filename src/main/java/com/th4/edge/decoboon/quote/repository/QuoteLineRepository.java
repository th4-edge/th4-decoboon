package com.th4.edge.decoboon.quote.repository;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

/**
 * 报价明细仓储。
 */
public interface QuoteLineRepository extends JpaRepository<QuoteLine, Long> {

    /**
     * 报价行本身没有工艺字段，施工工艺来自 catalogItem.craft，
     * 设计分项快照 designItem.criterion 兜底；两条关联一并预取，避免逐行懒加载。
     */
    @EntityGraph(attributePaths = {"catalogItem", "designItem"})
    List<QuoteLine> findByQuote(Quote quote);

    /** 财务汇总回退：一次取多报价的报价行（预取报价本体便于分组 + 目录项/设计分项）。 */
    @EntityGraph(attributePaths = {"quote", "catalogItem", "designItem"})
    List<QuoteLine> findByQuote_QuoteIdIn(java.util.Collection<Long> quoteIds);

    boolean existsByDesignItem_ItemId(Long itemId);

    boolean existsByPackageLine_ItemId(Long itemId);
}
