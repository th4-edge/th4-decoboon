package com.th4.edge.decoboon.catalog.repository;

import com.th4.edge.decoboon.catalog.model.CatalogItemRevision;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

/** 报价项目修订审计仓储。 */
public interface CatalogItemRevisionRepository extends JpaRepository<CatalogItemRevision, Long> {
    List<CatalogItemRevision> findByCompany_ComIdAndItemIdOrderByRevisionDesc(Long comId, Long itemId);

    @EntityGraph(attributePaths = "changedBy")
    Page<CatalogItemRevision> findByCompany_ComIdAndItemId(Long comId, Long itemId, Pageable pageable);
}
