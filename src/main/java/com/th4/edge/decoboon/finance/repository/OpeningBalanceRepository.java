package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.model.OpeningBalance;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 期初资金余额仓储。 */
public interface OpeningBalanceRepository extends JpaRepository<OpeningBalance, Long>,
        JpaSpecificationExecutor<OpeningBalance> {

    List<OpeningBalance> findByCompany_ComIdOrderByAsOfDateDescAccountAsc(Long comId);

    List<OpeningBalance> findByCompany_ComIdAndAsOfDateOrderByAccountAsc(Long comId, LocalDate asOfDate);

    Optional<OpeningBalance> findByCompany_ComIdAndAccountAndAsOfDate(Long comId, FundAccount account, LocalDate asOfDate);

    Optional<OpeningBalance> findByCompany_ComIdAndOpeningId(Long comId, Long openingId);
}
