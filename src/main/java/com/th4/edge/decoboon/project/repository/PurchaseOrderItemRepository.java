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

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"materialPlan", "materialPlan.catalogItem"})
    List<PurchaseOrderItem> findByPurchaseOrder_PurchaseIdIn(java.util.Collection<Long> purchaseIds);

    List<PurchaseOrderItem> findByMaterialPlanOrderByPurchaseItemIdAsc(MaterialPlan materialPlan);

    /** 材料计划工作台：一次取多计划的采购明细，替代逐计划查询。 */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"purchaseOrder"})
    List<PurchaseOrderItem> findByMaterialPlan_PlanIdIn(java.util.Collection<Long> planIds);

    List<PurchaseOrderItem> findByCompany_ComIdOrderByPurchaseItemIdDesc(Long comId);
}
