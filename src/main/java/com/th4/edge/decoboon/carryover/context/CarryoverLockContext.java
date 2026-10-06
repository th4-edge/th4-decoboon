package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;

import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.configure.CarryoverPlanStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 接管项目首笔业务锁定。 */
@Note("接管项目首笔业务锁定")
@Component
public class CarryoverLockContext {

    private final CarryoverProjectRepository projectRepository;

    private final DistributedLock distributedLock;

    public CarryoverLockContext(CarryoverProjectRepository projectRepository,
                DistributedLock distributedLock) {
        this.projectRepository = projectRepository;
        this.distributedLock = distributedLock;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void lock(Project project, CarryoverBusinessType businessType, Employee operator) {
        if (project == null || project.getProjectId() == null || businessType == null) {
            return;
        }
        String lockKey = "carryover-lock:" + operator.getCompany().getComId() + ":" + project.getProjectId();
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "承接项目正在锁定中，请稍后重试"));
        }
        try {
        CarryoverProject item = projectRepository.findByProject_ProjectId(project.getProjectId()).orElse(null);
        if (item == null || item.getStatus() == CarryoverProjectStatus.Locked
                || item.getStatus() == CarryoverProjectStatus.Cancelled) {
            return;
        }
        if (item.getStatus() != CarryoverProjectStatus.Applied) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        item.setLockedAt(now);
        item.setFirstBusinessAt(now);
        item.setFirstBusinessType(businessType);
        item.setLockedBy(operator);
        item.setStatus(CarryoverProjectStatus.Locked);
        if (item.getPlanStatus() == CarryoverPlanStatus.Confirmed
                || item.getPlanStatus() == CarryoverPlanStatus.Locked) {
            item.setPlanStatus(CarryoverPlanStatus.Locked);
        }
        projectRepository.save(item);
        } finally {
            distributedLock.unlock(lockKey);
        }
    }
}
