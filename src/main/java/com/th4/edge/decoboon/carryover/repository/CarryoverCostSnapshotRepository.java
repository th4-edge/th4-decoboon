package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.model.CarryoverCostSnapshot;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** 接管前成本快照仓储。 */
public interface CarryoverCostSnapshotRepository extends JpaRepository<CarryoverCostSnapshot, Long>,
        JpaSpecificationExecutor<CarryoverCostSnapshot> {

    Optional<CarryoverCostSnapshot> findByItem_ItemId(Long itemId);

    Optional<CarryoverCostSnapshot> findByProject_ProjectId(Long projectId);
}
