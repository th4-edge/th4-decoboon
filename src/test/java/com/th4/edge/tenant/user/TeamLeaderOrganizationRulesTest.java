package com.th4.edge.tenant.user;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.repository.CompanyJoinLimitRepository;
import com.th4.edge.tenant.user.repository.CompanyMemberLogRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.InviteRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-082：TeamLeader 只保留历史兼容，不得再被邀请、接受或分配。 */
class TeamLeaderOrganizationRulesTest {
    private UserIdentityContext userContext;
    private EmployeeRepository employeeRepository;
    private InviteRepository inviteRepository;
    private TeamRepository teamRepository;
    private CompanyMemberLogRepository memberLogRepository;
    private SubscriptionContext subscriptionContext;
    private ModelIdentityContext modelIdentityContext;
    private MembershipContext context;
    private Company company;
    private Store store;

    @BeforeEach
    void setUp() {
        userContext = mock(UserIdentityContext.class);
        employeeRepository = mock(EmployeeRepository.class);
        inviteRepository = mock(InviteRepository.class);
        teamRepository = mock(TeamRepository.class);
        memberLogRepository = mock(CompanyMemberLogRepository.class);
        subscriptionContext = mock(SubscriptionContext.class);
        modelIdentityContext = mock(ModelIdentityContext.class);
        context = new MembershipContext(
                userContext, employeeRepository, inviteRepository, teamRepository,
                mock(StoreRepository.class), memberLogRepository,
                mock(CompanyJoinLimitRepository.class), subscriptionContext,
                modelIdentityContext, mock(CustomerRepository.class),
                mock(PropertyRepository.class), mock(QuoteRepository.class), mock(ProjectRepository.class),
                mock(ProjectTaskRepository.class), mock(ReceivableRepository.class));
        company = new Company();
        company.setComId(1L);
        store = new Store();
        ReflectionTestUtils.setField(store, "storeId", 10L);
        store.setName("默认门店");
        store.setCompany(company);
        when(modelIdentityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inviteRepository.findByCompany_ComIdOrderByInviteIdDesc(1L)).thenReturn(List.of());
        when(inviteRepository.findByTelNoOrderByInviteIdDesc(any())).thenReturn(List.of());
        when(employeeRepository.countByCompany_ComIdAndStatus(1L, EmployeeStatus.Active)).thenReturn(0L);
        when(inviteRepository.countByCompany_ComIdAndStatus(1L, InviteStatus.Pending)).thenReturn(0L);
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void bossCannotInviteFrozenTeamLeaderRole() {
        Employee boss = employee(1L, EmployeeRole.Boss);
        assertThatThrownBy(() -> context.createInvite(boss, "团队负责人", "13900000006",
                EmployeeRole.TeamLeader, null, "历史团队"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
    }

    @Test
    void bossCanConvertHistoricalTeamLeaderToTargetRole() {
        Employee boss = employee(1L, EmployeeRole.Boss);
        Employee leader = employee(6L, EmployeeRole.TeamLeader);
        Team team = new Team();
        team.setTeamId(60L);
        team.setName("历史团队");
        team.setCompany(company);
        team.setStore(store);
        team.setLeader(leader);
        leader.setTeam(team);
        Credentials credentials = new Credentials();
        credentials.setEmployee(leader);
        when(employeeRepository.findByEmpId(6L)).thenReturn(leader);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContext.requireWx(leader)).thenReturn(credentials);
        when(memberLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Employee converted = context.updateMember(boss, 6L, EmployeeRole.Designer, null, null);

        assertThat(converted.getRole()).isEqualTo(EmployeeRole.Designer);
        assertThat(converted.getTeam()).isSameAs(team);
        assertThat(team.getLeader()).isSameAs(leader);
    }

    @Test
    void nonBossCannotConvertHistoricalTeamLeader() {
        Employee manager = employee(2L, EmployeeRole.StoreManager);
        Employee leader = employee(7L, EmployeeRole.TeamLeader);
        when(employeeRepository.findByEmpId(7L)).thenReturn(leader);
        assertThatThrownBy(() -> context.updateMember(manager, 7L, EmployeeRole.Designer, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅企业负责人可调整已冻结的团队负责人");
    }

    @Test
    void frozenTeamLeaderCannotAcceptInviteToSwitchCompany() {
        Invite invite = new Invite();
        invite.setInviteId(30L);
        invite.setCompany(company);
        invite.setTelNo("13900000007");
        invite.setRole(EmployeeRole.Marketer);
        invite.setStatus(InviteStatus.Pending);
        invite.setExpireTime(LocalDateTime.now().plusDays(1));
        Credentials credentials = new Credentials();
        credentials.setCreId(31L);
        credentials.setPhone(invite.getTelNo());
        Employee leader = employee(7L, EmployeeRole.TeamLeader);
        when(inviteRepository.findByInviteId(30L)).thenReturn(invite);
        when(userContext.identify(any(), any(), any(), any(), any())).thenReturn(credentials);
        when(userContext.lock(31L)).thenReturn(credentials);
        when(userContext.activeEmployee(credentials)).thenReturn(leader);

        assertThatThrownBy(() -> context.accept(30L, "open-id", "union-id", invite.getTelNo(),
                "新员工", true, "切换企业", null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
    }

    private Employee employee(Long id, EmployeeRole role) {
        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "empId", id);
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName("测试员工" + id);
        employee.setTelNo("1390000" + id);
        return employee;
    }
}
