package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 单项目业务承接仓储。 */
public interface CarryoverProjectRepository extends JpaRepository<CarryoverProject, Long>,
        JpaSpecificationExecutor<CarryoverProject> {

    List<CarryoverProject> findByBatch_BatchIdOrderByItemIdAsc(Long batchId);

    List<CarryoverProject> findByBatch_BatchIdAndStatusInOrderByItemIdAsc(
            Long batchId, Collection<CarryoverProjectStatus> statuses);

    long countByBatch_BatchIdAndStatus(Long batchId, CarryoverProjectStatus status);

    Optional<CarryoverProject> findByProject_ProjectId(Long projectId);

    Optional<CarryoverProject> findByItemIdAndCompany_ComId(Long itemId, Long comId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from CarryoverProject item where item.itemId = :itemId")
    Optional<CarryoverProject> findLockedByItemId(@Param("itemId") Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from CarryoverProject item where item.project.projectId = :projectId")
    Optional<CarryoverProject> findLockedByProjectId(@Param("projectId") Long projectId);
}
