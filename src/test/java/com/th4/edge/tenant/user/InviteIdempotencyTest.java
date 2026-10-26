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
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyJoinLimitRepository;
import com.th4.edge.tenant.user.repository.CompanyMemberLogRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.InviteRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InviteIdempotencyTest {
    private InviteRepository inviteRepository;
    private MembershipContext context;
    private Company company;
    private Store defaultStore;
    private Store secondStore;

    @BeforeEach
    void setUp() {
        inviteRepository = mock(InviteRepository.class);
        ModelIdentityContext identityContext = mock(ModelIdentityContext.class);
        context = new MembershipContext(
                mock(UserIdentityContext.class), mock(EmployeeRepository.class), inviteRepository,
                mock(TeamRepository.class), mock(StoreRepository.class), mock(CompanyMemberLogRepository.class),
                mock(CompanyJoinLimitRepository.class), mock(SubscriptionContext.class), identityContext,
                mock(CustomerRepository.class), mock(PropertyRepository.class), mock(QuoteRepository.class),
                mock(ProjectRepository.class), mock(ProjectTaskRepository.class), mock(ReceivableRepository.class));
        company = new Company();
        company.setComId(1L);
        defaultStore = store(10L, "总店", company);
        secondStore = store(20L, "二店", company);
        when(identityContext.prepare(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void duplicatePendingInviteReturnsExistingInviteIdempotently() {
        Employee boss = employee(1L, defaultStore, EmployeeRole.Boss);
        Invite existing = invite(41L, defaultStore, EmployeeRole.Designer, "13900000011",
                LocalDateTime.now().plusDays(3));
        when(inviteRepository.findFirstByCompany_ComIdAndTelNoAndStatusOrderByInviteIdDesc(
                1L, existing.getTelNo(), InviteStatus.Pending)).thenReturn(existing);

        Invite result = context.createInvite(boss, "重复提交", existing.getTelNo(),
                EmployeeRole.Designer, null, null);

        assertThat(result).isSameAs(existing);
        assertThat(result.getReusedPending()).isTrue();
        verify(inviteRepository, never()).save(any(Invite.class));
    }

    @Test
    void expiredPendingInviteIsClosedBeforeCreatingFreshInvite() {
        Employee boss = employee(1L, defaultStore, EmployeeRole.Boss);
        Invite expired = invite(42L, defaultStore, EmployeeRole.Designer, "13900000012",
                LocalDateTime.now().minusMinutes(1));
        when(inviteRepository.findFirstByCompany_ComIdAndTelNoAndStatusOrderByInviteIdDesc(
                1L, expired.getTelNo(), InviteStatus.Pending)).thenReturn(expired);

        Invite result = context.createInvite(boss, "新设计师", expired.getTelNo(),
                EmployeeRole.Designer, null, null);

        assertThat(expired.getStatus()).isEqualTo(InviteStatus.Expired);
        assertThat(result).isNotSameAs(expired);
        assertThat(result.getStatus()).isEqualTo(InviteStatus.Pending);
    }

    @Test
    void storeManagerCannotReuseCrossStorePendingInvite() {
        Employee manager = employee(2L, defaultStore, EmployeeRole.StoreManager);
        Invite existing = invite(43L, secondStore, EmployeeRole.Designer, "13900000013",
                LocalDateTime.now().plusDays(1));
        when(inviteRepository.findFirstByCompany_ComIdAndTelNoAndStatusOrderByInviteIdDesc(
                1L, existing.getTelNo(), InviteStatus.Pending)).thenReturn(existing);

        assertThatThrownBy(() -> context.createInvite(manager, "设计师", existing.getTelNo(),
                EmployeeRole.Designer, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("请联系原邀请人");
    }

    private Employee employee(Long id, Store store, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        return employee;
    }

    private Invite invite(Long id, Store store, EmployeeRole role, String phone, LocalDateTime expireTime) {
        Invite invite = new Invite();
        invite.setInviteId(id);
        invite.setCompany(company);
        invite.setStore(store);
        invite.setTelNo(phone);
        invite.setRole(role);
        invite.setStatus(InviteStatus.Pending);
        invite.setExpireTime(expireTime);
        return invite;
    }

    private Store store(Long id, String name, Company company) {
        Store store = new Store();
        store.setStoreId(id);
        store.setName(name);
        store.setCompany(company);
        return store;
    }
}
