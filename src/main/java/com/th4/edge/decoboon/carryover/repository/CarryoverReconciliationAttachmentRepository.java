package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationAttachment;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 双轨对账附件仓储。 */
public interface CarryoverReconciliationAttachmentRepository extends JpaRepository<CarryoverReconciliationAttachment, Long>,
        JpaSpecificationExecutor<CarryoverReconciliationAttachment> {

    List<CarryoverReconciliationAttachment> findByCompany_ComIdAndReconciliation_ReconciliationIdOrderByAttachmentIdAsc(
            Long comId, Long reconciliationId);
}
