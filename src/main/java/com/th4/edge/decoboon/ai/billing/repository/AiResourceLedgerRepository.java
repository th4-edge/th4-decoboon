package com.th4.edge.decoboon.ai.billing.repository;

import com.th4.edge.decoboon.ai.billing.configure.AiLedgerType;
import com.th4.edge.decoboon.ai.billing.model.AiResourceLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiResourceLedgerRepository extends JpaRepository<AiResourceLedger, Long> {
    List<AiResourceLedger> findByCompany_ComIdAndCall_CallIdAndType(Long comId, Long callId, AiLedgerType type);

    List<AiResourceLedger> findByCompany_ComIdAndCall_CallIdOrderByLedgerIdAsc(Long comId, Long callId);
}
