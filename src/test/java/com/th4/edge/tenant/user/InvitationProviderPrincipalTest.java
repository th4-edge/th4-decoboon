package com.th4.edge.tenant.user;

import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.MembershipContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.repository.InviteRepository;
import com.th4.edge.tenant.user.service.invite.invitation.cancel.support.Cancel;
import com.th4.edge.tenant.user.service.invite.invitation.page.support.Page;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitationProviderPrincipalTest {

    @Test
    void cancelLoadsFullEmployeeBeforeAuthorization() {
        TenantContext tenantContext = mock(TenantContext.class);
        MembershipContext membershipContext = mock(MembershipContext.class);
        Cancel provider = new Cancel(tenantContext, membershipContext);
        Employee principal = employee(1L, null, EmployeeRole.Boss);
        Employee current = employee(1L, company(), EmployeeRole.Boss);
        Invite invite = new Invite();
        invite.setInviteId(31L);
        ReflectionTestUtils.setField(provider, "principal", principal);
        ReflectionTestUtils.setField(provider, "inviteId", 31L);
        ReflectionTestUtils.setField(provider, "reason", "管理员取消邀请");
        when(tenantContext.requireEmployee(principal)).thenReturn(current);
        when(membershipContext.cancel(current, 31L, "管理员取消邀请")).thenReturn(invite);

        assertThat(provider.call(new Employee())).isSameAs(invite);
        verify(tenantContext).requireEmployee(principal);
    }

    @Test
    void noEmployeeCredentialGetsBusinessErrorInsteadOfTypeCastFailure() {
        Cancel cancel = new Cancel(mock(TenantContext.class), mock(MembershipContext.class));
        ReflectionTestUtils.setField(cancel, "principal", new Credentials());
        ReflectionTestUtils.setField(cancel, "inviteId", 31L);

        assertThatThrownBy(() -> cancel.call(new Employee()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("尚未加入企业");

        Page page = new Page(mock(TenantContext.class), mock(InviteRepository.class));
        ReflectionTestUtils.setField(page, "principal", new Credentials());
        assertThatThrownBy(() -> page.call(new Employee()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("尚未加入企业");
    }

    @Test
    void injectedPrincipalFieldsMustAcceptCredentialsAtFrameworkBoundary() throws Exception {
        Field pagePrincipal = Page.class.getDeclaredField("principal");
        Field cancelPrincipal = Cancel.class.getDeclaredField("principal");

        assertThat(pagePrincipal.getType()).isAssignableFrom(Credentials.class);
        assertThat(cancelPrincipal.getType()).isAssignableFrom(Credentials.class);
    }

    @Test
    void pageAllowsBossAndStoreManagerOnly() {
        TenantContext tenantContext = mock(TenantContext.class);
        InviteRepository inviteRepository = mock(InviteRepository.class);
        Employee boss = employee(1L, company(), EmployeeRole.Boss);
        Page page = new Page(tenantContext, inviteRepository);
        ReflectionTestUtils.setField(page, "principal", boss);
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 20);
        when(tenantContext.requireRole(boss, EmployeeRole.Boss, EmployeeRole.StoreManager)).thenReturn(boss);
        when(inviteRepository.findByCompany_ComId(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(page.call(new Employee()).getTotalElements()).isZero();
    }

    private Employee employee(Long id, Company company, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setCompany(company);
        employee.setRole(role);
        return employee;
    }

    private Company company() {
        Company company = new Company();
        company.setComId(1L);
        return company;
    }
}
