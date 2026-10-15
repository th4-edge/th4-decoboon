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

    List<MaterialSettlementItem> findByPurchaseItem_PurchaseItemIdIn(Collection<Long> purchaseItemIds);

    boolean existsByCompany_ComIdAndPurchaseItem_PurchaseItemId(Long comId, Long purchaseItemId);
}
