package com.th4.edge.tenant.user;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.common.context.StoreAttributionContext;
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
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.CompanyMemberLog;
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
import com.th4.edge.tenant.user.service.invite.invitation.cancel.support.Cancel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DEC-081：财务邀请目标门店、邀请角色边界与 BOSS 跨店调财务。 */
class FinanceOrganizationRulesTest {

    private UserIdentityContext userContext;
    private EmployeeRepository employeeRepository;
    private InviteRepository inviteRepository;
    private StoreRepository storeRepository;
    private CompanyMemberLogRepository memberLogRepository;
    private SubscriptionContext subscriptionContext;
    private StoreAttributionContext storeAttributionContext;
    private ModelIdentityContext modelIdentityContext;
    private MembershipContext context;

    private Company company;
    private Store defaultStore;
    private Store secondStore;

    @BeforeEach
    void setUp() {
        userContext = mock(UserIdentityContext.class);
        employeeRepository = mock(EmployeeRepository.class);
        inviteRepository = mock(InviteRepository.class);
        storeRepository = mock(StoreRepository.class);
        memberLogRepository = mock(CompanyMemberLogRepository.class);
        subscriptionContext = mock(SubscriptionContext.class);
        storeAttributionContext = mock(StoreAttributionContext.class);
        modelIdentityContext = mock(ModelIdentityContext.class);

        context = new MembershipContext(
                userContext, employeeRepository, inviteRepository, storeRepository,
                memberLogRepository, mock(CompanyJoinLimitRepository.class), subscriptionContext,
                storeAttributionContext, modelIdentityContext, mock(CustomerRepository.class),
                mock(PropertyRepository.class), mock(QuoteRepository.class), mock(ProjectRepository.class),
                mock(ProjectTaskRepository.class), mock(ReceivableRepository.class));

        company = new Company();
        company.setComId(1L);
        defaultStore = store(10L, "总店", true, company);
        secondStore = store(20L, "二店", false, company);

        when(modelIdentityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inviteRepository.findByCompany_ComIdOrderByInviteIdDesc(1L)).thenReturn(List.of());
        when(inviteRepository.findByTelNoOrderByInviteIdDesc(any())).thenReturn(List.of());
        when(employeeRepository.countByCompany_ComIdAndStatus(1L, EmployeeStatus.Active)).thenReturn(0L);
        when(inviteRepository.countByCompany_ComIdAndStatus(1L, InviteStatus.Pending)).thenReturn(0L);
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void bossMustChooseTargetStoreForFinanceInvite() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);

        assertThatThrownBy(() -> context.createInvite(boss, "总部财务", "13900000001",
                EmployeeRole.Finance, null, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("邀请财务必须选择归属门店");
    }

    @Test
    void bossFinanceInviteStoresSelectedTargetStore() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);
        when(storeAttributionContext.requireCompanyStore(20L, 1L)).thenReturn(secondStore);

        Invite invite = context.createInvite(boss, "二店财务", "13900000002",
                EmployeeRole.Finance, 20L, null, null);

        assertThat(invite.getStore()).isSameAs(secondStore);
        assertThat(invite.getRole()).isEqualTo(EmployeeRole.Finance);
    }

    @Test
    void storeManagerCanOnlyInviteOwnStoreFinance() {
        Employee manager = employee(2L, company, defaultStore, EmployeeRole.StoreManager);

        assertThatThrownBy(() -> context.createInvite(manager, "二店财务", "13900000003",
                EmployeeRole.Finance, 20L, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("店长只能邀请本店财务");
    }

    @Test
    void teamLeaderAndFinanceCannotInviteFinance() {
        Employee leader = employee(3L, company, defaultStore, EmployeeRole.TeamLeader);
        Employee finance = employee(4L, company, defaultStore, EmployeeRole.Finance);

        assertThatThrownBy(() -> context.createInvite(leader, "财务", "13900000004",
                EmployeeRole.Finance, null, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权邀请员工");
        assertThatThrownBy(() -> context.createInvite(finance, "财务", "13900000005",
                EmployeeRole.Finance, null, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权邀请员工");
    }

    @Test
    void bossCanMoveFinanceAcrossStores() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);
        Employee finance = employee(5L, company, defaultStore, EmployeeRole.Finance);
        Credentials credentials = new Credentials();
        credentials.setEmployee(finance);

        when(employeeRepository.findByEmpId(5L)).thenReturn(finance);
        when(storeAttributionContext.requireCompanyStore(20L, 1L)).thenReturn(secondStore);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userContext.requireWx(finance)).thenReturn(credentials);
        when(memberLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Employee moved = context.updateMember(boss, 5L, EmployeeRole.Finance, 20L, null);

        assertThat(moved.getStore()).isSameAs(secondStore);
        assertThat(moved.getRole()).isEqualTo(EmployeeRole.Finance);
    }

    @Test
    void bossCannotInviteFrozenTeamLeaderRole() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);

        assertThatThrownBy(() -> context.createInvite(boss, "团队负责人", "13900000006",
                EmployeeRole.TeamLeader, null, null, "历史团队"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
    }

    @Test
    void frozenTeamCannotBeAssignedOnInvite() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);

        assertThatThrownBy(() -> context.createInvite(boss, "设计师", "13900000008",
                EmployeeRole.Designer, null, 60L, "历史团队"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队已冻结");
    }

    @Test
    void frozenTeamCannotBeChangedOnMemberUpdate() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);
        Employee designer = employee(8L, company, defaultStore, EmployeeRole.Designer);
        when(employeeRepository.findByEmpId(8L)).thenReturn(designer);

        assertThatThrownBy(() -> context.updateMember(boss, 8L, EmployeeRole.Designer, null, 60L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队已冻结");
    }

    @Test
    void acceptingHistoricalInviteDoesNotCreateNewTeamAssignment() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);
        Employee historicalLeader = employee(9L, company, defaultStore, EmployeeRole.TeamLeader);
        Team historicalTeam = new Team();
        historicalTeam.setTeamId(60L);
        historicalTeam.setName("历史团队");
        historicalTeam.setCompany(company);
        historicalTeam.setStore(defaultStore);
        historicalTeam.setLeader(historicalLeader);

        Invite invite = new Invite();
        invite.setInviteId(31L);
        invite.setCompany(company);
        invite.setStore(defaultStore);
        invite.setTeam(historicalTeam);
        invite.setInviteName("新设计师");
        invite.setTelNo("13900000009");
        invite.setRole(EmployeeRole.Designer);
        invite.setStatus(InviteStatus.Pending);
        invite.setExpireTime(LocalDateTime.now().plusDays(1));

        Credentials credentials = new Credentials();
        credentials.setCreId(32L);
        credentials.setPhone(invite.getTelNo());

        when(inviteRepository.findByInviteId(31L)).thenReturn(invite);
        when(userContext.identify(any(), any(), any(), any(), any())).thenReturn(credentials);
        when(userContext.lock(32L)).thenReturn(credentials);
        when(userContext.activeEmployee(credentials)).thenReturn(null);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee accepted = context.accept(31L, "open-id", "union-id", invite.getTelNo(),
                "新设计师", false, null, null);

        assertThat(accepted.getTeam()).isNull();
        assertThat(invite.getTeam()).isSameAs(historicalTeam);
    }

    @Test
    void adminResignAuditMustRecordTheAdminOperator() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);
        Employee target = employee(8L, company, defaultStore, EmployeeRole.Designer);
        Credentials credentials = new Credentials();
        credentials.setEmployee(target);

        when(employeeRepository.findByEmpId(8L)).thenReturn(target);
        when(userContext.requireWx(target)).thenReturn(credentials);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberLogRepository.save(any(CompanyMemberLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.adminResign(boss, 8L, "验收离职", false);

        ArgumentCaptor<CompanyMemberLog> captor = ArgumentCaptor.forClass(CompanyMemberLog.class);
        verify(memberLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOperator()).isSameAs(boss);
        assertThat(captor.getValue().getEmployee()).isSameAs(target);
    }

    @Test
    void cancelProviderMustLoadFullTenantEmployeeBeforeAuthorization() {
        TenantContext tenantContext = mock(TenantContext.class);
        MembershipContext membershipContext = mock(MembershipContext.class);
        Cancel cancel = new Cancel(tenantContext, membershipContext);
        Employee principal = employee(1L, null, null, EmployeeRole.Boss);
        Employee current = employee(1L, company, defaultStore, EmployeeRole.Boss);
        Invite invite = new Invite();
        invite.setInviteId(31L);

        ReflectionTestUtils.setField(cancel, "employee", principal);
        ReflectionTestUtils.setField(cancel, "inviteId", 31L);
        ReflectionTestUtils.setField(cancel, "reason", "验收取消");
        when(tenantContext.requireEmployee(principal)).thenReturn(current);
        when(membershipContext.cancel(current, 31L, "验收取消")).thenReturn(invite);

        assertThat(cancel.call(new Employee())).isSameAs(invite);
        verify(tenantContext).requireEmployee(principal);
        verify(membershipContext).cancel(current, 31L, "验收取消");
    }

    @Test
    void resumeMustValidateSeatCapacityAndStoreAttribution() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);
        Employee suspended = employee(5L, company, defaultStore, EmployeeRole.Designer);
        suspended.setStatus(EmployeeStatus.Suspended);

        when(employeeRepository.findByEmpId(5L)).thenReturn(suspended);
        when(userContext.requireWx(suspended)).thenReturn(null);
        when(employeeRepository.countByCompany_ComIdAndStatus(1L, EmployeeStatus.Active)).thenReturn(5L);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        context.resume(boss, 5L);

        verify(storeAttributionContext).requireAttributable(defaultStore);
        verify(subscriptionContext).requireWritable(company);
        verify(subscriptionContext).requireSeatCapacity(company, 5L);
    }

    @Test
    void bossCanConvertHistoricalTeamLeaderToTargetRole() {
        Employee boss = employee(1L, company, defaultStore, EmployeeRole.Boss);
        Employee leader = employee(6L, company, defaultStore, EmployeeRole.TeamLeader);
        Team team = new Team();
        team.setTeamId(60L);
        team.setName("历史团队");
        team.setCompany(company);
        team.setStore(defaultStore);
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
        assertThat(converted.getStore()).isSameAs(defaultStore);
        assertThat(converted.getTeam()).isSameAs(team);
        assertThat(team.getLeader()).isSameAs(leader);
    }

    @Test
    void storeManagerCannotPromoteAnotherEmployeeToStoreManager() {
        Employee manager = employee(2L, company, defaultStore, EmployeeRole.StoreManager);
        Employee designer = employee(8L, company, defaultStore, EmployeeRole.Designer);
        when(employeeRepository.findByEmpId(8L)).thenReturn(designer);

        assertThatThrownBy(() -> context.updateMember(manager, 8L, EmployeeRole.StoreManager, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("只有企业负责人可以设置店长");
    }

    @Test
    void nonBossCannotConvertHistoricalTeamLeader() {
        Employee manager = employee(2L, company, defaultStore, EmployeeRole.StoreManager);
        Employee leader = employee(7L, company, defaultStore, EmployeeRole.TeamLeader);
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
        Employee leader = employee(7L, company, defaultStore, EmployeeRole.TeamLeader);

        when(inviteRepository.findByInviteId(30L)).thenReturn(invite);
        when(userContext.identify(any(), any(), any(), any(), any())).thenReturn(credentials);
        when(userContext.lock(31L)).thenReturn(credentials);
        when(userContext.activeEmployee(credentials)).thenReturn(leader);

        assertThatThrownBy(() -> context.accept(30L, "open-id", "union-id", invite.getTelNo(),
                "新员工", true, "切换企业", null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
    }

    @Test
    void storeManagerCannotMoveFinanceAcrossStores() {
        Employee manager = employee(2L, company, defaultStore, EmployeeRole.StoreManager);
        Employee finance = employee(5L, company, defaultStore, EmployeeRole.Finance);

        when(employeeRepository.findByEmpId(5L)).thenReturn(finance);
        when(storeAttributionContext.requireCompanyStore(20L, 1L)).thenReturn(secondStore);

        assertThatThrownBy(() -> context.updateMember(manager, 5L, EmployeeRole.Finance, 20L, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("仅企业负责人可跨店调整员工");
    }

    private Store store(Long id, String name, boolean defaultStore, Company company) {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "storeId", id);
        store.setName(name);
        store.setDefaultStore(defaultStore);
        store.setCompany(company);
        return store;
    }

    private Employee employee(Long id, Company company, Store store, EmployeeRole role) {
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
