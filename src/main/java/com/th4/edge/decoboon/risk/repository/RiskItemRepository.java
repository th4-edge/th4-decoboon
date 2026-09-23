package com.th4.edge.decoboon.risk.repository;

import com.th4.edge.decoboon.risk.configure.RiskStatus;
import com.th4.edge.decoboon.risk.model.RiskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RiskItemRepository extends JpaRepository<RiskItem, Long>, JpaSpecificationExecutor<RiskItem> {

    Optional<RiskItem> findByCompany_ComIdAndAlertKey(Long comId, String alertKey);

    List<RiskItem> findByCompany_ComIdAndStatusIn(Long comId, Collection<RiskStatus> statuses);

    List<RiskItem> findByCompany_ComIdAndRiskIdIn(Long comId, Collection<Long> riskIds);

    List<RiskItem> findByCompany_ComIdAndStatus(Long comId, RiskStatus status);
}
