package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 变更报价明细仓储。 */
public interface ContractChangeItemRepository extends JpaRepository<ContractChangeItem, Long>,
        JpaSpecificationExecutor<ContractChangeItem> {

    List<ContractChangeItem> findByChange_ChangeIdOrderByChangeItemIdAsc(Long changeId);

    List<ContractChangeItem> findByChange_ChangeIdInOrderByChangeItemIdAsc(Collection<Long> changeIds);

    /** 当前有效剩余投影：一次按合同加载变更明细及其来源/目标关系。 */
    @EntityGraph(attributePaths = {"change", "sourceLine", "sourceChangeItem", "targetCatalogItem"})
    List<ContractChangeItem> findByChange_Contract_ContractIdOrderByChangeItemIdAsc(Long contractId);

    /** 按企业范围加载已生效变更目标来源。 */
    @EntityGraph(attributePaths = {"change", "sourceLine", "sourceChangeItem", "targetCatalogItem"})
    Optional<ContractChangeItem> findByChangeItemIdAndCompany_ComId(Long changeItemId, Long comId);
}
