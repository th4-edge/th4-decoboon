package com.th4.edge.decoboon.carryover.context;

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

    public CarryoverLockContext(CarryoverProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void lock(Project project, CarryoverBusinessType businessType, Employee operator) {
        if (project == null || project.getProjectId() == null || businessType == null) {
            return;
        }
        CarryoverProject item = projectRepository.findLockedByProjectId(project.getProjectId()).orElse(null);
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
    }
}
