package com.th4.edge.tenant.user;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.UserSecurityLogRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserIdentityContextTest {

    @Test
    void shouldReuseExistingWxCredentialAndNotCreateDuplicateUser() {
        CredentialsRepository credentials = mock(CredentialsRepository.class);
        UserIdentityContext context;
        Credentials existing = credential(1L, "open-1", "13800000000");
        ModelIdentityContext identity = mock(ModelIdentityContext.class);
        when(identity.prepare(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        context = new UserIdentityContext(credentials, mock(EmployeeRepository.class),
                mock(UserSecurityLogRepository.class), identity);
        when(credentials.findByTypeAndIdKey(CredType.Wx, "open-1")).thenReturn(existing);
        when(credentials.save(existing)).thenReturn(existing);

        Credentials result = context.identify("open-1", "union-1", "13800000000", null, null);

        assertThat(result).isSameAs(existing);
        assertThat(result.getIdSecret()).isEqualTo("union-1");
        verify(credentials).save(existing);
    }

    @Test
    void shouldRejectPhoneBoundToDifferentOpenId() {
        CredentialsRepository credentials = mock(CredentialsRepository.class);
        UserIdentityContext context = new UserIdentityContext(credentials,
                mock(EmployeeRepository.class), mock(UserSecurityLogRepository.class),
                mock(ModelIdentityContext.class));
        when(credentials.findByTypeAndIdKey(CredType.Wx, "new-open")).thenReturn(null);
        when(credentials.findByTypeAndPhone(CredType.Wx, "13800000000"))
                .thenReturn(credential(2L, "other-open", "13800000000"));

        assertThatThrownBy(() -> context.identify("new-open", null, "13800000000", null, null))
                .hasMessageContaining("已绑定其他微信");
    }

    private Credentials credential(Long id, String openId, String phone) {
        Credentials credential = new Credentials();
        credential.setCreId(id);
        credential.setType(CredType.Wx);
        credential.setIdKey(openId);
        credential.setPhone(phone);
        return credential;
    }
}
