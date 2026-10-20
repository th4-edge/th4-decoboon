package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.ReceiptReversal;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** 收款冲正仓储。 */
public interface ReceiptReversalRepository extends JpaRepository<ReceiptReversal, Long>,
        JpaSpecificationExecutor<ReceiptReversal> {

    Optional<ReceiptReversal> findByCompany_ComIdAndReceipt_ReceiptId(Long comId, Long receiptId);

    Optional<ReceiptReversal> findByCompany_ComIdAndIdempotencyKey(Long comId, String idempotencyKey);
}
