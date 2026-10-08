package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.plan.context.ContractPlanContext;
import com.th4.edge.decoboon.project.configure.PlanDeviationStatus;
import com.th4.edge.decoboon.project.configure.PlanDeviationStrategy;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.collab.context.SubscribeMessageContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectPlanDeviation;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectPlanDeviationRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.service.plandeviations.update.support.Update;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DEC-051 计划偏差确认/驳回/客户原因路由回归。 */
class PlanDeviationUpdateTest {

    private ProjectPlanDeviationRepository deviationRepository;
    private ProjectContractRepository contractRepository;
    private ProjectReportRepository reportRepository;
    private ProjectAccessContext accessContext;
    private ProjectPermissionContext permissionContext;
    private ContractPlanContext contractPlanContext;
    private ProjectContext projectContext;
    private SubscribeMessageContext subscribeMessageContext;
    private Update update;
    private Employee boss;
    private ProjectPlanDeviation deviation;
    private ProjectContract contract;

    @BeforeEach
    void setUp() {
        deviationRepository = mock(ProjectPlanDeviationRepository.class);
        contractRepository = mock(ProjectContractRepository.class);
        reportRepository = mock(ProjectReportRepository.class);
        accessContext = mock(ProjectAccessContext.class);
        permissionContext = mock(ProjectPermissionContext.class);
        contractPlanContext = mock(ContractPlanContext.class);
        projectContext = mock(ProjectContext.class);
        subscribeMessageContext = mock(SubscribeMessageContext.class);
        update = new Update(deviationRepository, contractRepository, reportRepository,
                accessContext, permissionContext, contractPlanContext, projectContext, subscribeMessageContext);

        Company company = new Company();
        company.setComId(100L);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setName("张伟");
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);

        Project project = new Project();
        project.setProjectId(9001L);
        project.setCompany(company);

        contract = new ProjectContract();
        contract.setContractId(7001L);
        contract.setCompany(company);

        deviation = new ProjectPlanDeviation();
        deviation.setDeviationId(6001L);
        deviation.setProject(project);
        deviation.setContract(contract);
        deviation.setBaseEndDate(LocalDate.of(2026, 12, 1));
        deviation.setExpectedEndDate(LocalDate.of(2026, 12, 21));
        deviation.setStatus(PlanDeviationStatus.Pending);

        when(accessContext.requireEmployee(boss)).thenReturn(boss);
        when(accessContext.requireVisible(project, boss)).thenReturn(project);
        when(accessContext.requireMemberEditor(project, boss)).thenReturn(project);
        when(deviationRepository.findById(6001L)).thenReturn(Optional.of(deviation));
        when(deviationRepository.save(any(ProjectPlanDeviation.class))).thenAnswer(i -> i.getArgument(0));
        when(contractRepository.save(any(ProjectContract.class))).thenAnswer(i -> i.getArgument(0));
        ReflectionTestUtils.setField(update, "employee", boss);
        ReflectionTestUtils.setField(update, "deviationId", 6001L);
    }

    @Test
    void confirmWithExtendMovesContractBaseline() {
        ReflectionTestUtils.setField(update, "status", PlanDeviationStatus.Confirmed);
        ReflectionTestUtils.setField(update, "scheduleStrategy", PlanDeviationStrategy.Extend);

        ProjectPlanDeviation result = update.call(new ProjectPlanDeviation());

        assertThat(result.getStatus()).isEqualTo(PlanDeviationStatus.Confirmed);
        assertThat(result.getNewBaseEndDate()).isEqualTo(LocalDate.of(2026, 12, 21));
        assertThat(contract.getPlanEndDate()).isEqualTo(LocalDate.of(2026, 12, 21));
        verify(contractPlanContext).updateBaselineEndDate(eq(contract), eq(LocalDate.of(2026, 12, 21)));
        verify(projectContext).recalculateReceivableDueDates(contract);
        verify(subscribeMessageContext).ownerPlanDeviationConfirmed(result);
        assertThat(result.getReceivableRecalculated()).isTrue();
        verify(reportRepository).save(any());
    }

    @Test
    void confirmWithCompressKeepsContractBaseline() {
        ReflectionTestUtils.setField(update, "status", PlanDeviationStatus.Confirmed);
        ReflectionTestUtils.setField(update, "scheduleStrategy", PlanDeviationStrategy.Compress);

        ProjectPlanDeviation result = update.call(new ProjectPlanDeviation());

        assertThat(result.getNewBaseEndDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(contract.getPlanEndDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void confirmWithoutStrategyIsRejected() {
        ReflectionTestUtils.setField(update, "status", PlanDeviationStatus.Confirmed);
        ReflectionTestUtils.setField(update, "scheduleStrategy", null);

        assertThatThrownBy(() -> update.call(new ProjectPlanDeviation()))
                .hasMessageContaining("请先选择顺延方式");
    }

    @Test
    void customerReasonRoutesToContractChangeWithoutMovingBaseline() {
        ReflectionTestUtils.setField(update, "status", PlanDeviationStatus.RoutedToChange);

        ProjectPlanDeviation result = update.call(new ProjectPlanDeviation());

        assertThat(result.getStatus()).isEqualTo(PlanDeviationStatus.RoutedToChange);
        assertThat(contract.getPlanEndDate()).isNull();
    }

    @Test
    void processedDeviationCannotBeConfirmedTwice() {
        deviation.setStatus(PlanDeviationStatus.Confirmed);
        ReflectionTestUtils.setField(update, "status", PlanDeviationStatus.Confirmed);
        ReflectionTestUtils.setField(update, "scheduleStrategy", PlanDeviationStrategy.Extend);

        assertThatThrownBy(() -> update.call(new ProjectPlanDeviation()))
                .hasMessageContaining("不能重复确认");
    }
}
