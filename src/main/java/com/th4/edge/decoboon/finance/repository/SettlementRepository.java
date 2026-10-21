package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 项目结算仓储。 */
public interface SettlementRepository extends JpaRepository<Settlement, Long>,
        JpaSpecificationExecutor<Settlement> {

    Optional<Settlement> findByProject_ProjectId(Long projectId);

    Optional<Settlement> findByProject_ProjectIdAndCompany_ComId(Long projectId, Long comId);

    List<Settlement> findByCompany_ComIdAndProject_ProjectIdIn(Long comId, Collection<Long> projectIds);
}
