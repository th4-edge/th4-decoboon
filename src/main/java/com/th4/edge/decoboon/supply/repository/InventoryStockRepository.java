package com.th4.edge.decoboon.supply.repository;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.supply.model.InventoryStock;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 库存仓储。 */
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    List<InventoryStock> findByCompany_ComIdOrderByStockIdDesc(Long comId);
    InventoryStock findFirstByCompany_ComIdAndItemAndBatchNoOrderByStockIdDesc(Long comId, CatalogItem item, String batchNo);
    List<InventoryStock> findByCompany_ComIdAndItemOrderByStockIdAsc(Long comId, CatalogItem item);
}
