package com.th4.edge.decoboon.carryover.repository;

import com.th4.edge.decoboon.carryover.configure.CarryoverBatchStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 在跑项目承接任务仓储。 */
public interface CarryoverBatchRepository extends JpaRepository<CarryoverBatch, Long>,
        JpaSpecificationExecutor<CarryoverBatch> {

    Optional<CarryoverBatch> findFirstByCompany_ComIdAndStatusOrderByBatchIdDesc(
            Long comId, CarryoverBatchStatus status);

    List<CarryoverBatch> findByCompany_ComIdOrderByBatchIdDesc(Long comId);

    Optional<CarryoverBatch> findByBatchIdAndCompany_ComId(Long batchId, Long comId);
}
