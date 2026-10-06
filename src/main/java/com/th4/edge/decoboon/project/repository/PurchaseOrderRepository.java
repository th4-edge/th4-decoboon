package com.th4.edge.decoboon.project.repository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,Long>,
        JpaSpecificationExecutor<PurchaseOrder>{
 List<PurchaseOrder> findByProjectOrderByPurchaseIdDesc(Project project);

 /** 财务汇总：一次取多项目采购单（预取报价行与所属项目），替代逐项目查询。 */
 @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"quoteLine", "project"})
 List<PurchaseOrder> findByProject_ProjectIdInOrderByPurchaseIdDesc(java.util.Collection<Long> projectIds);
 List<PurchaseOrder> findByMaterialPlanOrderByPurchaseIdAsc(com.th4.edge.decoboon.project.model.MaterialPlan materialPlan);
 List<PurchaseOrder> findByCompany_ComIdOrderByPurchaseIdDesc(Long comId);
}
