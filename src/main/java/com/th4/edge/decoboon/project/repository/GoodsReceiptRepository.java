package com.th4.edge.decoboon.project.repository;
import com.th4.edge.decoboon.project.model.GoodsReceipt;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt,Long>,
        JpaSpecificationExecutor<GoodsReceipt>{
 List<GoodsReceipt> findByPurchaseOrderByReceiptIdDesc(PurchaseOrder purchase);
}
