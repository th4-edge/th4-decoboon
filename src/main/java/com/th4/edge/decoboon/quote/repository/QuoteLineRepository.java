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
     * 报价行优先使用自身 craft 快照；目录项和设计分项用于历史/补充展示。
     * 区域和组件投影一并预取，避免在无事务边界时逐行懒加载。
     */
    @EntityGraph(attributePaths = {"catalogItem", "designItem", "space", "components"})
    List<QuoteLine> findByQuote(Quote quote);

    boolean existsByDesignItem_ItemId(Long itemId);

    boolean existsByPackageLine_ItemId(Long itemId);
}
