package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.MaterialReturn;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 材料退货单仓储。 */
public interface MaterialReturnRepository extends JpaRepository<MaterialReturn, Long>,
        JpaSpecificationExecutor<MaterialReturn> {

    List<MaterialReturn> findByProject_ProjectIdOrderByMaterialReturnIdDesc(Long projectId);

    List<MaterialReturn> findByMaterialPlanOrderByMaterialReturnIdAsc(MaterialPlan materialPlan);

    List<MaterialReturn> findByPurchaseOrderOrderByMaterialReturnIdAsc(PurchaseOrder purchaseOrder);

    List<MaterialReturn> findByCompany_ComIdOrderByMaterialReturnIdDesc(Long comId);
}
