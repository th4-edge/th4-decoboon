package com.th4.edge.decoboon.project.repository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,Long>,
        JpaSpecificationExecutor<PurchaseOrder>{
 List<PurchaseOrder> findByProjectOrderByPurchaseIdDesc(Project project);
 List<PurchaseOrder> findByMaterialPlanOrderByPurchaseIdAsc(com.th4.edge.decoboon.project.model.MaterialPlan materialPlan);
 List<PurchaseOrder> findByCompany_ComIdOrderByPurchaseIdDesc(Long comId);
}
