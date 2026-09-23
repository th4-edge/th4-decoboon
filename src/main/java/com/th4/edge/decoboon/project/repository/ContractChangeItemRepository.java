package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 变更报价明细仓储。 */
public interface ContractChangeItemRepository extends JpaRepository<ContractChangeItem, Long>,
        JpaSpecificationExecutor<ContractChangeItem> {

    List<ContractChangeItem> findByChange_ChangeIdOrderByChangeItemIdAsc(Long changeId);

    List<ContractChangeItem> findByChange_ChangeIdInOrderByChangeItemIdAsc(Collection<Long> changeIds);
}
