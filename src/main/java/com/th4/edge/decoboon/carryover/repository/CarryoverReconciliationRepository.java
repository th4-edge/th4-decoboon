package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliation;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 首月双轨对账仓储。 */
public interface CarryoverReconciliationRepository extends JpaRepository<CarryoverReconciliation, Long>,
        JpaSpecificationExecutor<CarryoverReconciliation> {

    List<CarryoverReconciliation> findByCompany_ComIdOrderByReconciliationIdDesc(Long comId);

    Optional<CarryoverReconciliation> findByCompany_ComIdAndReconciliationId(Long comId, Long reconciliationId);

    Optional<CarryoverReconciliation> findFirstByCompany_ComIdAndBatch_BatchIdOrderByReconciliationIdDesc(
            Long comId, Long batchId);
}
