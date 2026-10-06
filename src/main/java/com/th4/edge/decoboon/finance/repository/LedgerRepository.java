package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 资金流水仓储。 */
public interface LedgerRepository extends JpaRepository<Ledger, Long>,
        JpaSpecificationExecutor<Ledger> {

    @EntityGraph(attributePaths = {"project", "owner", "store", "team", "categoryNode"})
    List<Ledger> findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(Long comId);

    List<Ledger> findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(Long comId, Long projectId);

    /** 财务汇总：一次取多项目流水（手工账口径仍按 source/purpose 过滤）。 */
    List<Ledger> findByCompany_ComIdAndProject_ProjectIdIn(Long comId, java.util.Collection<Long> projectIds);

    /** 财务汇总展示：按 id 批量重取，预取响应实际使用的关联，避免序列化期逐条懒加载。 */
    @EntityGraph(attributePaths = {"project", "project.property", "project.customer", "category", "categoryNode"})
    List<Ledger> findByCompany_ComIdAndLedgerIdIn(Long comId, java.util.Collection<Long> ledgerIds);

    java.util.Optional<Ledger> findByCompany_ComIdAndLedgerId(Long comId, Long ledgerId);
}
