package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 接管前成本快照仓储。 */
public interface CarryoverCostSnapshotRepository extends JpaRepository<CarryoverCostSnapshot, Long>,
        JpaSpecificationExecutor<CarryoverCostSnapshot> {

    Optional<CarryoverCostSnapshot> findByItem_ItemId(Long itemId);

    Optional<CarryoverCostSnapshot> findByProject_ProjectId(Long projectId);

    /** 财务汇总等聚合读取：一次取多项目快照（预取 project 便于分组），替代逐项目查询。 */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"project"})
    List<CarryoverCostSnapshot> findByProject_ProjectIdIn(java.util.Collection<Long> projectIds);
}
