package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReceiptRefundRepository extends JpaRepository<ReceiptRefund, Long>,
        JpaSpecificationExecutor<ReceiptRefund> {

    Optional<ReceiptRefund> findByCompany_ComIdAndIdempotencyKey(Long comId, String idempotencyKey);

    /** 分页后的批量详情读取：一次预取页面白名单关联，避免逐行懒加载。 */
    @EntityGraph(attributePaths = {"project", "receipt", "customer"})
    List<ReceiptRefund> findByRefundIdIn(Collection<Long> refundIds);

    List<ReceiptRefund> findByCompany_ComIdAndProject_ProjectIdOrderByRefundAtDescRefundIdDesc(
            Long comId, Long projectId);

    List<ReceiptRefund> findByCompany_ComIdAndReceipt_ReceiptIdOrderByRefundAtDescRefundIdDesc(
            Long comId, Long receiptId);
}
