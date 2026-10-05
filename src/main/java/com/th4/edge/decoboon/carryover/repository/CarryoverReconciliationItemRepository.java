package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.configure.CarryoverReconciliationTheme;
import com.th4.edge.decoboon.carryover.model.CarryoverReconciliationItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 双轨对账主题仓储。 */
public interface CarryoverReconciliationItemRepository extends JpaRepository<CarryoverReconciliationItem, Long>,
        JpaSpecificationExecutor<CarryoverReconciliationItem> {

    List<CarryoverReconciliationItem> findByCompany_ComIdAndReconciliation_ReconciliationIdOrderByThemeAsc(
            Long comId, Long reconciliationId);

    Optional<CarryoverReconciliationItem> findByCompany_ComIdAndItemId(Long comId, Long itemId);

    Optional<CarryoverReconciliationItem> findByCompany_ComIdAndReconciliation_ReconciliationIdAndTheme(
            Long comId, Long reconciliationId, CarryoverReconciliationTheme theme);
}
