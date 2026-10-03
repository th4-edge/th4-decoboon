package com.th4.edge.decoboon.ai.repository;

import com.th4.edge.decoboon.ai.model.AiUsageCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** AI 调用底账仓储。 */
public interface AiUsageCallRepository extends JpaRepository<AiUsageCall, Long>, JpaSpecificationExecutor<AiUsageCall> {

    Optional<AiUsageCall> findByCompany_ComIdAndIdempotencyKey(Long comId, String idempotencyKey);
}
