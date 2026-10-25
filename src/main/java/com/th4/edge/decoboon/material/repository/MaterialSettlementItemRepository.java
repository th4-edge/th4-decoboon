package com.th4.edge.decoboon.material.repository;

import com.th4.edge.decoboon.material.model.MaterialSettlementItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 主材采购结算明细仓储。 */
public interface MaterialSettlementItemRepository extends JpaRepository<MaterialSettlementItem, Long>,
        JpaSpecificationExecutor<MaterialSettlementItem> {

    List<MaterialSettlementItem> findBySettlement_SettlementIdOrderBySettlementItemIdAsc(Long settlementId);

    /** 成本投影批量读取：预取结算头状态与采购明细，避免循环内懒加载。 */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"settlement", "purchaseItem"})
    List<MaterialSettlementItem> findByPurchaseItem_PurchaseItemIdIn(Collection<Long> purchaseItemIds);

    boolean existsByCompany_ComIdAndPurchaseItem_PurchaseItemId(Long comId, Long purchaseItemId);
}
