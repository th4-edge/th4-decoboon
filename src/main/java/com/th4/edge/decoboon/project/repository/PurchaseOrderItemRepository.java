package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.model.PurchaseOrderItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 采购单明细仓储。 */
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long>,
        JpaSpecificationExecutor<PurchaseOrderItem> {

    List<PurchaseOrderItem> findByPurchaseOrderOrderByPurchaseItemIdAsc(PurchaseOrder purchaseOrder);

    List<PurchaseOrderItem> findByMaterialPlanOrderByPurchaseItemIdAsc(MaterialPlan materialPlan);

    List<PurchaseOrderItem> findByCompany_ComIdOrderByPurchaseItemIdDesc(Long comId);
}
