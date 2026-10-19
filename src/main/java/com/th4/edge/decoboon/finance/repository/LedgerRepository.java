package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 资金流水仓储。 */
public interface LedgerRepository extends JpaRepository<Ledger, Long>,
        JpaSpecificationExecutor<Ledger> {

    @EntityGraph(attributePaths = {"project", "owner", "store", "team", "categoryNode"})
    List<Ledger> findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(Long comId);

    List<Ledger> findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(Long comId, Long projectId);

    /** 财务汇总：一次取多项目流水（手工账口径仍按 source/purpose 过滤）。 */
    List<Ledger> findByCompany_ComIdAndProject_ProjectIdIn(Long comId, java.util.Collection<Long> projectIds);

    List<Ledger> findByCompany_ComIdAndLedgerIdIn(Long comId, java.util.Collection<Long> ledgerIds);

    java.util.Optional<Ledger> findByCompany_ComIdAndLedgerId(Long comId, Long ledgerId);

    /**
     * 门店经营利润组件（MET-011）：门店非项目收入/直接归属企业费用。
     * 只统计未作废、无项目关联、手工用途明确的流水。
     */
    @Query("""
            select coalesce(sum(l.amount), 0)
            from Ledger l
            where l.company.comId = :comId
              and l.store.storeId = :storeId
              and l.voided = false
              and l.project is null
              and l.direction = :direction
              and l.purpose = :purpose
            """)
    java.math.BigDecimal sumStoreLedger(@Param("comId") Long comId,
                                        @Param("storeId") Long storeId,
                                        @Param("direction") com.th4.edge.decoboon.finance.configure.LedgerDirection direction,
                                        @Param("purpose") com.th4.edge.decoboon.finance.configure.LedgerManualPurpose purpose);
}
