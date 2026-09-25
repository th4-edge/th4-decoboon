package com.th4.edge.decoboon.catalog.repository;

import com.th4.edge.decoboon.catalog.configure.CatalogImportRowStatus;
import com.th4.edge.decoboon.catalog.model.CatalogImportRow;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 报价项目库导入行仓储。 */
public interface CatalogImportRowRepository extends JpaRepository<CatalogImportRow, Long>,
        JpaSpecificationExecutor<CatalogImportRow> {

    List<CatalogImportRow> findByBatch_BatchIdOrderByRowNoAsc(Long batchId);

    List<CatalogImportRow> findByBatch_BatchIdAndStatusInOrderByRowNoAsc(Long batchId,
                                                                        Collection<CatalogImportRowStatus> statuses);

    long countByBatch_BatchIdAndStatus(Long batchId, CatalogImportRowStatus status);

    /** 导入行关联的项目（删除项目时需要解除关联并保留批次审计）。 */
    List<CatalogImportRow> findByItem_ItemId(Long itemId);
}
