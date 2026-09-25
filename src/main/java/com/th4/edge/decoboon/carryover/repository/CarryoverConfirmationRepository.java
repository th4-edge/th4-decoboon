package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.configure.CarryoverConfirmationRole;
import com.th4.edge.decoboon.carryover.model.CarryoverConfirmation;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 承接项目确认记录仓储。 */
public interface CarryoverConfirmationRepository extends JpaRepository<CarryoverConfirmation, Long>,
        JpaSpecificationExecutor<CarryoverConfirmation> {

    List<CarryoverConfirmation> findByItem_ItemIdOrderByConfirmationIdAsc(Long itemId);

    Optional<CarryoverConfirmation> findByItem_ItemIdAndRequiredRoleAndConfirmedVersion(
            Long itemId, CarryoverConfirmationRole requiredRole, Integer confirmedVersion);
}
