package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.AcceptanceRound;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 阶段验收整改轮次仓储（append-only）。
 */
public interface AcceptanceRoundRepository extends JpaRepository<AcceptanceRound, Long>,
        JpaSpecificationExecutor<AcceptanceRound> {

    List<AcceptanceRound> findByAcceptanceOrderByRoundNoDesc(Acceptance acceptance);

    Optional<AcceptanceRound> findFirstByAcceptanceOrderByRoundNoDesc(Acceptance acceptance);

    long countByAcceptance(Acceptance acceptance);

    List<AcceptanceRound> findByCompany_ComIdAndAcceptance_AcceptanceIdOrderByRoundNoDesc(Long comId, Long acceptanceId);
}
