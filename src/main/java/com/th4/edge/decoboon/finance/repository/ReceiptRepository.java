package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 收款仓储。 */
public interface ReceiptRepository extends JpaRepository<Receipt, Long>,
        JpaSpecificationExecutor<Receipt> {

    List<Receipt> findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(Long comId, Long projectId);

    List<Receipt> findByCompany_ComIdOrderByReceivedAtDesc(Long comId);

    List<Receipt> findTop200ByCompany_ComIdOrderByReceivedAtDesc(Long comId);

    Optional<Receipt> findByCompany_ComIdAndIdempotencyKey(Long comId, String idempotencyKey);
}
