package com.th4.edge.decoboon.statistic.repository;

import com.th4.edge.decoboon.statistic.model.BusinessTarget;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/** 企业月度经营目标仓储。 */
public interface BusinessTargetRepository extends JpaRepository<BusinessTarget, Long> {

    Optional<BusinessTarget> findByCompany_ComIdAndTargetMonth(Long comId, LocalDate targetMonth);
}
