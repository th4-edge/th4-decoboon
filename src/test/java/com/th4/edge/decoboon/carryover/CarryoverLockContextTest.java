package com.th4.edge.decoboon.carryover;

import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.configure.CarryoverPlanStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.components.lock.DistributedLock;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CarryoverLockContextTest {

    @Test
    void shouldLockConfirmedRemainingPlanOnFirstBusiness() {
        CarryoverProjectRepository repository = mock(CarryoverProjectRepository.class);
        Project project = new Project();
        project.setProjectId(10L);
        CarryoverProject item = new CarryoverProject();
        item.setItemId(20L);
        item.setStatus(CarryoverProjectStatus.Applied);
        item.setPlanStatus(CarryoverPlanStatus.Confirmed);
        when(repository.findByProject_ProjectId(10L)).thenReturn(Optional.of(item));

        new CarryoverLockContext(repository, noopLock()).lock(project, CarryoverBusinessType.Task, operator());

        assertThat(item.getStatus()).isEqualTo(CarryoverProjectStatus.Locked);
        assertThat(item.getPlanStatus()).isEqualTo(CarryoverPlanStatus.Locked);
    }

    @Test
    void shouldNotPretendUnconfirmedPlanWasConfirmedOnLock() {
        CarryoverProjectRepository repository = mock(CarryoverProjectRepository.class);
        Project project = new Project();
        project.setProjectId(10L);
        CarryoverProject item = new CarryoverProject();
        item.setItemId(20L);
        item.setStatus(CarryoverProjectStatus.Applied);
        item.setPlanStatus(CarryoverPlanStatus.Draft);
        when(repository.findByProject_ProjectId(10L)).thenReturn(Optional.of(item));

        new CarryoverLockContext(repository, noopLock()).lock(project, CarryoverBusinessType.Task, operator());

        assertThat(item.getStatus()).isEqualTo(CarryoverProjectStatus.Locked);
        assertThat(item.getPlanStatus()).isEqualTo(CarryoverPlanStatus.Draft);
    }

    private static DistributedLock noopLock() {
        return new DistributedLock() {
            @Override public boolean tryLock(String name, long waitMillis, long leaseMillis) { return true; }
            @Override public void unlock(String name) { }
        };
    }

    private static Employee operator() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setCompany(company);
        return employee;
    }
}
