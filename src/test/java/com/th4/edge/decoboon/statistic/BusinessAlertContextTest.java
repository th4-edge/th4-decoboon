package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.context.BusinessAlertContext;
import com.th4.edge.decoboon.statistic.model.BusinessAlertQuery;
import com.th4.edge.decoboon.statistic.model.BusinessAlertReport;
import com.th4.edge.decoboon.supply.configure.StockMovementType;
import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.decoboon.supply.repository.StockMovementRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Team;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessAlertContextTest {

    @Test
    void shouldDetectFourBusinessRiskTypes() {
        TenantContext tenant = mock(TenantContext.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectContractRepository contracts = mock(ProjectContractRepository.class);
        ReceivableRepository receivables = mock(ReceivableRepository.class);
        StockMovementRepository movements = mock(StockMovementRepository.class);
        ContractChangeRepository changes = mock(ContractChangeRepository.class);
        BusinessAlertContext context = new BusinessAlertContext(tenant, projects, contracts,
                receivables, movements, changes);

        LocalDate asOf = LocalDate.of(2026, 9, 14);
        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setEmpId(2L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        Project project = new Project();
        project.setProjectId(3L);
        project.setNo("P-001");
        project.setName("测试项目");
        project.setCompany(company);
        project.setStatus(ProjectStatus.Suspended);
        project.setManager(boss);

        Receivable receivable = new Receivable();
        receivable.setReceivableId(4L);
        receivable.setProject(project);
        receivable.setName("尾款");
        receivable.setAmount(new BigDecimal("5000.00"));
        receivable.setReceivedAmount(new BigDecimal("1000.00"));
        receivable.setDueDate(asOf.minusDays(35));

        MaterialPlan plan = new MaterialPlan();
        plan.setPlanId(7L);
        plan.setQuantity(new BigDecimal("10"));
        StockMovement issue = new StockMovement();
        issue.setMovementId(5L);
        issue.setSourceNo("LL-001");
        issue.setProject(project);
        issue.setMaterialPlan(plan);
        issue.setType(StockMovementType.Issue);
        issue.setQuantity(new BigDecimal("12"));
        issue.setUnitCost(new BigDecimal("100.00"));

        ProjectContract contract = new ProjectContract();
        contract.setContractId(6L);
        contract.setNo("HT-001");
        contract.setProject(project);
        contract.setBaselineAmount(new BigDecimal("10000.00"));
        contract.setCurrentAmount(new BigDecimal("10000.00"));
        ContractChange change = new ContractChange();
        change.setProject(project);
        change.setContract(contract);
        change.setStatus(ContractChangeStatus.Approved);
        change.setAmount(new BigDecimal("1500.00"));

        when(tenant.requireRole(boss, EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager, EmployeeRole.TeamLeader))
                .thenReturn(boss);
        when(projects.findByCompany_ComIdOrderByProjectIdDesc(1L)).thenReturn(List.of(project));
        when(contracts.findByCompany_ComIdOrderByContractIdDesc(1L)).thenReturn(List.of(contract));
        when(receivables.findByCompany_ComIdOrderByDueDateAsc(1L)).thenReturn(List.of(receivable));
        when(movements.findByCompany_ComIdOrderByMovementIdDesc(1L)).thenReturn(List.of(issue));
        when(changes.findByCompany_ComIdOrderByChangeIdDesc(1L)).thenReturn(List.of(change));

        BusinessAlertQuery query = new BusinessAlertQuery();
        query.setAsOf(asOf);
        BusinessAlertReport report = context.report(boss, query);

        assertThat(report.getTotalCount()).isEqualTo(4);
        assertThat(report.getOverdueReceivables()).extracting("type")
                .containsExactly(BusinessAlertType.OverdueReceivable);
        assertThat(report.getOverdueReceivables().get(0).getAmount()).isEqualByComparingTo("4000.00");
        assertThat(report.getSuspensionRisks()).extracting("type")
                .containsExactly(BusinessAlertType.ProjectSuspensionRisk);
        assertThat(report.getMaterialOverIssues().get(0).getRate()).isEqualByComparingTo("20.0");
        assertThat(report.getChangeAnomalies().get(0).getRate()).isEqualByComparingTo("15.0");
    }

    @Test
    void teamLeaderSeesOnlyOwnTeamRiskQueue() {
        TenantContext tenant = mock(TenantContext.class);
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectContractRepository contracts = mock(ProjectContractRepository.class);
        ReceivableRepository receivables = mock(ReceivableRepository.class);
        StockMovementRepository movements = mock(StockMovementRepository.class);
        ContractChangeRepository changes = mock(ContractChangeRepository.class);
        BusinessAlertContext context = new BusinessAlertContext(tenant, projects, contracts,
                receivables, movements, changes);

        Company company = new Company();
        company.setComId(1L);
        Team teamA = new Team();
        teamA.setTeamId(10L);
        Team teamB = new Team();
        teamB.setTeamId(20L);
        Employee leader = new Employee();
        leader.setEmpId(2L);
        leader.setRole(EmployeeRole.TeamLeader);
        leader.setCompany(company);
        leader.setTeam(teamA);

        Project ownTeam = new Project();
        ownTeam.setProjectId(3L);
        ownTeam.setNo("P-A");
        ownTeam.setName("本团队项目");
        ownTeam.setCompany(company);
        ownTeam.setTeam(teamA);
        ownTeam.setStatus(ProjectStatus.Suspended);
        Project otherTeam = new Project();
        otherTeam.setProjectId(4L);
        otherTeam.setNo("P-B");
        otherTeam.setName("跨团队项目");
        otherTeam.setCompany(company);
        otherTeam.setTeam(teamB);
        otherTeam.setStatus(ProjectStatus.Suspended);

        when(tenant.requireRole(leader, EmployeeRole.Boss, EmployeeRole.Finance,
                EmployeeRole.StoreManager, EmployeeRole.TeamLeader)).thenReturn(leader);
        when(projects.findByCompany_ComIdOrderByProjectIdDesc(1L)).thenReturn(List.of(otherTeam, ownTeam));
        when(contracts.findByCompany_ComIdOrderByContractIdDesc(1L)).thenReturn(List.of());
        when(receivables.findByCompany_ComIdOrderByDueDateAsc(1L)).thenReturn(List.of());
        when(movements.findByCompany_ComIdOrderByMovementIdDesc(1L)).thenReturn(List.of());
        when(changes.findByCompany_ComIdOrderByChangeIdDesc(1L)).thenReturn(List.of());

        BusinessAlertReport report = context.report(leader, new BusinessAlertQuery());

        assertThat(report.getSuspensionRisks()).extracting("project.projectId").containsExactly(3L);
    }
}
