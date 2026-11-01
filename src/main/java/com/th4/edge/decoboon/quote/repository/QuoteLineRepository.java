package com.th4.edge.decoboon.quote.repository;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/** 报价明细仓储。 */
public interface QuoteLineRepository extends JpaRepository<QuoteLine, Long>,
        JpaSpecificationExecutor<QuoteLine> {

    @EntityGraph(attributePaths = {"catalogItem", "designItem", "space", "components"})
    List<QuoteLine> findByQuote(Quote quote);

    @Override
    @EntityGraph(attributePaths = {"space"})
    Page<QuoteLine> findAll(Specification<QuoteLine> specification, Pageable pageable);

    boolean existsByDesignItem_ItemId(Long itemId);

    boolean existsByPackageLine_ItemId(Long itemId);
}
