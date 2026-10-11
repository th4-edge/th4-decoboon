package com.th4.edge.decoboon.owner;

import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.service.auth.session.devownerlogin.support.DevOwnerLogin;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 本地开发业主会话（auth/session/dev-owner-login）的安全边界回归。
 *
 * <p>该入口只用于本地验证业主链路，必须：默认关闭、非 dev 关闭、不关联员工、且存在项目授权或待接受邀请。</p>
 */
class DevOwnerLoginGuardTest {

    private DevOwnerLogin provider(CredentialsRepository credentials, CustomerAccessRepository accesses,
                                   CustomerInviteRepository invites, boolean enabled, String profiles) {
        DevOwnerLogin provider = new DevOwnerLogin(credentials, accesses, invites);
        ReflectionTestUtils.setField(provider, "enabled", enabled);
        ReflectionTestUtils.setField(provider, "activeProfiles", profiles);
        ReflectionTestUtils.setField(provider, "phone", "13910008888");
        return provider;
    }

    @Test
    void shouldBeInertWhenDisabledOrNotDevProfile() {
        CredentialsRepository credentials = mock(CredentialsRepository.class);
        CustomerAccessRepository accesses = mock(CustomerAccessRepository.class);
        CustomerInviteRepository invites = mock(CustomerInviteRepository.class);

        DevOwnerLogin disabled = provider(credentials, accesses, invites, false, "dev");
        assertThatThrownBy(() -> disabled.call(new Credentials()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("接口不存在");

        DevOwnerLogin prodProfile = provider(credentials, accesses, invites, true, "prod");
        assertThatThrownBy(() -> prodProfile.call(new Credentials()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("接口不存在");
    }

    @Test
    void shouldRejectEmployeeBoundCredential() {
        CredentialsRepository credentials = mock(CredentialsRepository.class);
        CustomerAccessRepository accesses = mock(CustomerAccessRepository.class);
        CustomerInviteRepository invites = mock(CustomerInviteRepository.class);
        Credentials credential = new Credentials();
        credential.setCreId(1L);
        credential.setPhone("13910008888");
        credential.setStatus(UserStatus.Active);
        credential.setEmployee(new Employee());
        when(credentials.findByTypeAndPhone(CredType.Wx, "13910008888")).thenReturn(credential);

        DevOwnerLogin provider = provider(credentials, accesses, invites, true, "dev");
        assertThatThrownBy(() -> provider.call(new Credentials()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已绑定员工账号");
    }

    @Test
    void shouldRequireProjectAccessOrPendingInvite() {
        CredentialsRepository credentials = mock(CredentialsRepository.class);
        CustomerAccessRepository accesses = mock(CustomerAccessRepository.class);
        CustomerInviteRepository invites = mock(CustomerInviteRepository.class);
        Credentials credential = new Credentials();
        credential.setCreId(9L);
        credential.setPhone("13910008888");
        credential.setStatus(UserStatus.Active);
        when(credentials.findByTypeAndPhone(CredType.Wx, "13910008888")).thenReturn(credential);
        when(accesses.findByCredential_CreIdAndStatusOrderByAccessIdAsc(eq(9L), eq(CustomerAccessStatus.Active)))
                .thenReturn(List.of());
        when(invites.findByTelNoOrderByInviteIdDesc("13910008888")).thenReturn(List.of());

        DevOwnerLogin provider = provider(credentials, accesses, invites, true, "dev");
        assertThatThrownBy(() -> provider.call(new Credentials()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("尚未授权任何项目");
    }

    @Test
    void shouldIssueSessionForAuthorizedOwner() {
        CredentialsRepository credentials = mock(CredentialsRepository.class);
        CustomerAccessRepository accesses = mock(CustomerAccessRepository.class);
        CustomerInviteRepository invites = mock(CustomerInviteRepository.class);
        Credentials credential = new Credentials();
        credential.setCreId(9L);
        credential.setPhone("13910008888");
        credential.setStatus(UserStatus.Active);
        when(credentials.findByTypeAndPhone(CredType.Wx, "13910008888")).thenReturn(credential);
        CustomerAccess access = new CustomerAccess();
        when(accesses.findByCredential_CreIdAndStatusOrderByAccessIdAsc(anyLong(), any()))
                .thenReturn(List.of(access));

        DevOwnerLogin provider = provider(credentials, accesses, invites, true, "dev");
        Credentials result = provider.call(new Credentials());
        assertThat(result.getCreId()).isEqualTo(9L);
    }
}
