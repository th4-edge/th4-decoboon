package com.th4.edge.decoboon.carryover;

import com.th4.edge.decoboon.carryover.configure.CarryoverPlanAction;
import com.th4.edge.decoboon.carryover.configure.CarryoverPlanStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverPlanContext;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
import com.th4.edge.decoboon.carryover.model.CarryoverPlanChange;
import com.th4.edge.decoboon.carryover.model.CarryoverPlanConfirmation;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverPlanChangeRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverPlanConfirmationRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CarryoverPlanContextTest {

    @Test
    void shouldCreateFutureStageAndTrackPlanDraft() {
        Fixture f = fixture(CarryoverProjectStatus.Applied, CarryoverPlanStatus.NotStarted);
        when(f.stageRepository.save(any(ProjectStage.class))).thenAnswer(invocation -> {
            ProjectStage stage = invocation.getArgument(0);
            stage.setStageId(10L);
            return stage;
        });

        ProjectStage saved = f.context.saveStage(f.operator, f.item.getItemId(), null, "泥瓦施工",
                "MASONRY", null, LocalDate.of(2026, 9, 27), LocalDate.of(2026, 10, 8), null);

        assertThat(saved.getName()).isEqualTo("泥瓦施工");
        assertThat(saved.getStageCode()).isEqualTo("MASONRY");
        assertThat(saved.getStatus()).isEqualTo(StageStatus.Todo);
        assertThat(f.item.getPlanStatus()).isEqualTo(CarryoverPlanStatus.Draft);
        assertThat(f.item.getPlanVersion()).isEqualTo(1);
        verify(f.changeRepository).save(any());
    }

    @Test
    void shouldKeepDraftWhenLockedProjectHasNotConfirmedPlanYet() {
        Fixture f = fixture(CarryoverProjectStatus.Locked, CarryoverPlanStatus.NotStarted);
        when(f.stageRepository.save(any(ProjectStage.class))).thenAnswer(invocation -> {
            ProjectStage stage = invocation.getArgument(0);
            stage.setStageId(11L);
            return stage;
        });

        f.context.saveStage(f.operator, f.item.getItemId(), null, "安装收尾",
                "INSTALL", null, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 16),
                "接管后首笔业务已发生，继续补充剩余计划");

        assertThat(f.item.getPlanStatus()).isEqualTo(CarryoverPlanStatus.Draft);
        assertThat(f.item.getPlanVersion()).isEqualTo(1);
        ArgumentCaptor<CarryoverPlanChange> changeCaptor = ArgumentCaptor.forClass(CarryoverPlanChange.class);
        verify(f.changeRepository).save(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getReason()).isEqualTo("接管后首笔业务已发生，继续补充剩余计划");
    }

    @Test
    void shouldDeleteFutureStageAndKeepAuditTrace() {
        Fixture f = fixture(CarryoverProjectStatus.Applied, CarryoverPlanStatus.Draft);
        ProjectStage future = new ProjectStage();
        future.setStageId(2L);
        future.setProject(f.project);
        future.setName("木作施工");
        future.setStageCode("CARPENTRY");
        future.setSort(1);
        future.setStatus(StageStatus.Todo);
        future.setProgress(0);
        when(f.stageRepository.findById(2L)).thenReturn(java.util.Optional.of(future));
        when(f.stageRepository.findByProjectOrderBySort(f.project))
                .thenReturn(List.of(f.baseline, future));

        f.context.deleteStage(f.operator, f.item.getItemId(), 2L, null);

        verify(f.stageRepository).delete(future);
        ArgumentCaptor<CarryoverPlanChange> changeCaptor = ArgumentCaptor.forClass(CarryoverPlanChange.class);
        verify(f.changeRepository).save(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getAction()).isEqualTo(CarryoverPlanAction.StageDelete);
        assertThat(changeCaptor.getValue().getStageName()).isEqualTo("木作施工");
    }

    @Test
    void shouldReorderFutureStagesWithoutChangingBaseline() {
        Fixture f = fixture(CarryoverProjectStatus.Applied, CarryoverPlanStatus.Draft);
        ProjectStage first = new ProjectStage();
        first.setStageId(2L);
        first.setProject(f.project);
        first.setName("泥瓦施工");
        first.setSort(1);
        ProjectStage second = new ProjectStage();
        second.setStageId(3L);
        second.setProject(f.project);
        second.setName("木作施工");
        second.setSort(2);
        List<ProjectStage> stages = java.util.List.of(f.baseline, first, second);
        when(f.stageRepository.findByProjectOrderBySort(f.project)).thenReturn(stages);

        f.context.reorder(f.operator, f.item.getItemId(), List.of(3L, 2L), null);

        assertThat(f.baseline.getSort()).isZero();
        assertThat(second.getSort()).isEqualTo(1);
        assertThat(first.getSort()).isEqualTo(2);
    }

    @Test
    void shouldRejectDeletingCarryoverBaselineStage() {
        Fixture f = fixture(CarryoverProjectStatus.Applied, CarryoverPlanStatus.Draft);
        ProjectStage baseline = new ProjectStage();
        baseline.setStageId(1L);
        baseline.setProject(f.project);
        baseline.setStageCode("CARRYOVER_BASELINE");
        baseline.setName("水电施工");
        when(f.stageRepository.findById(1L)).thenReturn(java.util.Optional.of(baseline));

        DefinedException error = assertThrows(DefinedException.class,
                () -> f.context.deleteStage(f.operator, f.item.getItemId(), 1L, null));
        assertThat(error.getMessage()).contains("接管基线阶段不能删除");
    }

    @Test
    void shouldRequireReasonWhenLockedPlanIsAdjusted() {
        Fixture f = fixture(CarryoverProjectStatus.Locked, CarryoverPlanStatus.Locked);
        ProjectStage stage = new ProjectStage();
        stage.setStageId(2L);
        stage.setProject(f.project);
        stage.setName("木作施工");
        stage.setStageCode("CARPENTRY");
        stage.setStatus(StageStatus.Todo);
        stage.setProgress(0);
        stage.setSort(1);
        when(f.stageRepository.findById(2L)).thenReturn(java.util.Optional.of(stage));

        DefinedException error = assertThrows(DefinedException.class,
                () -> f.context.saveStage(f.operator, f.item.getItemId(), 2L, "木作施工",
                        "CARPENTRY", 1, LocalDate.of(2026, 10, 9),
                        LocalDate.of(2026, 10, 20), null));
        assertThat(error.getMessage()).contains("必须填写原因");
    }

    @Test
    void shouldConfirmCurrentPlanVersionWithProxyTrace() {
        Fixture f = fixture(CarryoverProjectStatus.Applied, CarryoverPlanStatus.Draft);
        when(f.itemRepository.save(any(CarryoverProject.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(f.stageRepository.findByProjectOrderBySort(f.project)).thenReturn(List.of(f.baseline));

        CarryoverProject result = f.context.confirm(f.operator, f.item.getItemId(), "BOSS代项目经理确认");

        assertThat(result.getPlanStatus()).isEqualTo(CarryoverPlanStatus.Confirmed);
        ArgumentCaptor<CarryoverPlanConfirmation> captor = ArgumentCaptor.forClass(CarryoverPlanConfirmation.class);
        verify(f.confirmationRepository).save(captor.capture());
        assertThat(captor.getValue().getProxy()).isTrue();
        assertThat(captor.getValue().getRequiredRole().getValue()).isEqualTo("Pm");
    }

    private Fixture fixture(CarryoverProjectStatus itemStatus, CarryoverPlanStatus planStatus) {
        CarryoverAccessContext accessContext = mock(CarryoverAccessContext.class);
        CarryoverProjectRepository itemRepository = mock(CarryoverProjectRepository.class);
        CarryoverPlanConfirmationRepository confirmationRepository = mock(CarryoverPlanConfirmationRepository.class);
        CarryoverPlanChangeRepository changeRepository = mock(CarryoverPlanChangeRepository.class);
        ProjectAccessContext projectAccessContext = mock(ProjectAccessContext.class);
        ProjectPermissionContext permissionContext = mock(ProjectPermissionContext.class);
        ProjectStageRepository stageRepository = mock(ProjectStageRepository.class);
        ProjectTaskRepository taskRepository = mock(ProjectTaskRepository.class);
        AcceptanceRepository acceptanceRepository = mock(AcceptanceRepository.class);
        ProjectContext projectContext = mock(ProjectContext.class);

        Company company = new Company();
        company.setComId(1L);
        Employee operator = new Employee();
        operator.setEmpId(9L);
        operator.setRole(EmployeeRole.Boss);
        operator.setCompany(company);

        Project project = new Project();
        project.setProjectId(100L);
        project.setCompany(company);

        CarryoverBatch batch = new CarryoverBatch();
        batch.setAsOfDate(LocalDate.of(2026, 9, 26));

        CarryoverProject item = new CarryoverProject();
        item.setItemId(200L);
        item.setCompany(company);
        item.setStatus(itemStatus);
        item.setPlanStatus(planStatus);
        item.setPlanVersion(0);
        item.setBatch(batch);
        item.setProject(project);

        ProjectStage baseline = new ProjectStage();
        baseline.setStageId(1L);
        baseline.setProject(project);
        baseline.setStageCode("CARRYOVER_BASELINE");
        baseline.setName("水电施工");
        baseline.setSort(0);

        when(accessContext.requireEditor(operator)).thenReturn(operator);
        when(itemRepository.findByItemIdAndCompany_ComId(item.getItemId(), company.getComId()))
                .thenReturn(java.util.Optional.of(item));
        when(projectAccessContext.requirePlanEditor(project, operator)).thenReturn(project);
        when(stageRepository.findByProjectOrderBySort(project)).thenReturn(List.of(baseline));
        when(taskRepository.findByStage(any(ProjectStage.class))).thenReturn(List.of());
        when(acceptanceRepository.findFirstByStageOrderByAcceptanceIdDesc(any(ProjectStage.class)))
                .thenReturn(java.util.Optional.empty());

        CarryoverPlanContext context = new CarryoverPlanContext(accessContext, itemRepository,
                confirmationRepository, changeRepository, projectAccessContext, permissionContext,
                stageRepository, taskRepository, acceptanceRepository, projectContext);
        return new Fixture(context, itemRepository, confirmationRepository, changeRepository,
                stageRepository, operator, item, project, baseline);
    }

    private record Fixture(CarryoverPlanContext context,
                           CarryoverProjectRepository itemRepository,
                           CarryoverPlanConfirmationRepository confirmationRepository,
                           CarryoverPlanChangeRepository changeRepository,
                           ProjectStageRepository stageRepository,
                           Employee operator,
                           CarryoverProject item,
                           Project project,
                           ProjectStage baseline) {
    }
}
