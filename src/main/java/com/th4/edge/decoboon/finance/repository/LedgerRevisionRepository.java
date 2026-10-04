package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.LedgerRevision;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 手工流水更正审计仓储。 */
public interface LedgerRevisionRepository extends JpaRepository<LedgerRevision, Long>,
        JpaSpecificationExecutor<LedgerRevision> {

    List<LedgerRevision> findByCompany_ComIdAndLedger_LedgerIdOrderByRevisionIdDesc(Long comId, Long ledgerId);
}
