package com.th4.edge.decoboon.material.repository;

import com.th4.edge.decoboon.material.configure.SettlementScope;
import com.th4.edge.decoboon.material.model.MaterialSettlement;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 主材采购结算单仓储。 */
public interface MaterialSettlementRepository extends JpaRepository<MaterialSettlement, Long>,
        JpaSpecificationExecutor<MaterialSettlement> {

    List<MaterialSettlement> findByCompany_ComIdOrderBySettlementIdDesc(Long comId);

    Optional<MaterialSettlement> findFirstBySettlementIdAndCompany_ComId(Long settlementId, Long comId);

    List<MaterialSettlement> findByCompany_ComIdAndSupplier_PartnerIdOrderBySettlementIdDesc(Long comId, Long partnerId);

    List<MaterialSettlement> findByCompany_ComIdAndScopeAndPeriodOrderBySettlementIdDesc(
            Long comId, SettlementScope scope, String period);
}
