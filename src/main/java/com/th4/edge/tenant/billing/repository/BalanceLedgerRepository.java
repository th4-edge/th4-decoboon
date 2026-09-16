package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.BalanceLedger;

import java.util.List;

/**
 * 余额流水仓储。
 */
public interface BalanceLedgerRepository extends JpaRepository<BalanceLedger, Long> {

    List<BalanceLedger> findByCompany_ComId(Long comId);
}
