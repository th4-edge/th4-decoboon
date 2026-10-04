package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 资金流水仓储。 */
public interface LedgerRepository extends JpaRepository<Ledger, Long>,
        JpaSpecificationExecutor<Ledger> {

    List<Ledger> findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(Long comId);

    List<Ledger> findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(Long comId, Long projectId);

    List<Ledger> findByCompany_ComIdAndLedgerIdIn(Long comId, java.util.Collection<Long> ledgerIds);

    java.util.Optional<Ledger> findByCompany_ComIdAndLedgerId(Long comId, Long ledgerId);
}
