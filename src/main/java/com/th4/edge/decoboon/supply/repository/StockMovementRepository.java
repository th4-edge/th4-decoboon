package com.th4.edge.decoboon.supply.repository;

import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 库存流水仓储。 */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByCompany_ComIdOrderByMovementIdDesc(Long comId);
    List<StockMovement> findByProject_ProjectIdAndCompany_ComIdOrderByMovementIdDesc(Long projectId, Long comId);
}
