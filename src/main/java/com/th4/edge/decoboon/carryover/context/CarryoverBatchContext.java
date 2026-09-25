package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.decoboon.carryover.configure.CarryoverBatchStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
import com.th4.edge.decoboon.carryover.repository.CarryoverBatchRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 承接任务维护。 */
@Note("承接任务维护")
@Component
public class CarryoverBatchContext {

    private final CarryoverBatchRepository batchRepository;

    public CarryoverBatchContext(CarryoverBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public CarryoverBatch info(Employee current) {
        return batchRepository.findFirstByCompany_ComIdAndStatusOrderByBatchIdDesc(
                        current.getCompany().getComId(), CarryoverBatchStatus.Active)
                .orElseGet(() -> {
                    CarryoverBatch empty = new CarryoverBatch();
                    empty.setCompany(current.getCompany());
                    empty.setCreatedBy(current);
                    empty.setAsOfDate(LocalDate.now());
                    empty.setStatus(CarryoverBatchStatus.Active);
                    return empty;
                });
    }

    public CarryoverBatch save(Employee current, CarryoverBatch request) {
        if (request == null) {
            throw new DefinedException(new DefaultErrorCode("400", "承接任务不能为空"));
        }
        CarryoverBatch target;
        if (request.getBatchId() == null) {
            target = batchRepository.findFirstByCompany_ComIdAndStatusOrderByBatchIdDesc(
                            current.getCompany().getComId(), CarryoverBatchStatus.Active)
                    .orElseGet(CarryoverBatch::new);
            target.setCompany(current.getCompany());
            target.setCreatedBy(current);
            target.setStatus(CarryoverBatchStatus.Active);
            if (target.getVersion() == null) target.setVersion(1);
        } else {
            target = batchRepository.findByBatchIdAndCompany_ComId(request.getBatchId(), current.getCompany().getComId())
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接任务不存在或无权访问")));
            if (target.getStatus() != CarryoverBatchStatus.Active) {
                throw new DefinedException(new DefaultErrorCode("400", "已结束的承接任务不能修改"));
            }
        }
        if (request.getAsOfDate() != null) {
            target.setAsOfDate(request.getAsOfDate());
        } else if (target.getAsOfDate() == null) {
            target.setAsOfDate(LocalDate.now());
        }
        if (request.getScopeConfirmedAt() != null) {
            target.setScopeConfirmedAt(request.getScopeConfirmedAt());
        }
        return batchRepository.save(target);
    }

    public CarryoverBatch complete(Employee current, Long batchId) {
        CarryoverBatch batch = batchRepository.findByBatchIdAndCompany_ComId(batchId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接任务不存在或无权访问")));
        batch.setStatus(CarryoverBatchStatus.Completed);
        batch.setCompletedAt(LocalDateTime.now());
        return batchRepository.save(batch);
    }
}
