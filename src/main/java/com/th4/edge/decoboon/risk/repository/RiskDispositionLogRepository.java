package com.th4.edge.decoboon.risk.repository;

import com.th4.edge.decoboon.risk.model.RiskDispositionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RiskDispositionLogRepository extends JpaRepository<RiskDispositionLog, Long>,
        JpaSpecificationExecutor<RiskDispositionLog> {

    List<RiskDispositionLog> findByRisk_RiskIdOrderByLogIdAsc(Long riskId);
}
