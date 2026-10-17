package com.th4.edge.decoboon.project.repository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,Long>,
        JpaSpecificationExecutor<PurchaseOrder>{
 List<PurchaseOrder> findByProjectOrderByPurchaseIdDesc(Project project);
 Optional<PurchaseOrder> findByNo(String no);

 /** 财务汇总：一次取多项目采购单（预取报价行与所属项目），替代逐项目查询。 */
 @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"quoteLine", "project"})
 List<PurchaseOrder> findByProject_ProjectIdInOrderByPurchaseIdDesc(java.util.Collection<Long> projectIds);
 List<PurchaseOrder> findByMaterialPlanOrderByPurchaseIdAsc(com.th4.edge.decoboon.project.model.MaterialPlan materialPlan);

 /** 材料计划工作台：一次取多计划的采购单，替代逐计划查询。 */
 List<PurchaseOrder> findByMaterialPlan_PlanIdIn(java.util.Collection<Long> planIds);
 List<PurchaseOrder> findByCompany_ComIdOrderByPurchaseIdDesc(Long comId);

 /** 供应商 + 企业维度取采购单（月结候选集用）。 */
 List<PurchaseOrder> findByCompany_ComIdAndSupplier_PartnerIdOrderByPurchaseIdDesc(Long comId, Long partnerId);
}
