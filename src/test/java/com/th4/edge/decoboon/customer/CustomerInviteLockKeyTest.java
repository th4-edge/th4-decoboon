package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerInviteContext;
import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 业主邀请分布式锁键口径（Batch 5）。
 *
 * <p>锁键错误会直接导致并发保护失效（一次性邀请可被重复接受、撤销与接受交错），因此单独固化：
 * token 与 inviteId 必须解析到同一把邀请锁；接受（登录身份）与撤销（acceptedBy）必须解析到同一把授权锁。</p>
 */
class CustomerInviteLockKeyTest {

    private final CustomerInviteRepository inviteRepository = mock(CustomerInviteRepository.class);

    private final CustomerInviteContext context = new CustomerInviteContext(
            inviteRepository,
            mock(CustomerAccessRepository.class),
            mock(UserIdentityContext.class),
            mock(ProjectAccessContext.class));

    @Test
    void inviteKeyResolvesTokenAndInviteIdToSameLock() {
        Project project = project(7001L);
        CustomerInvite invite = invite(9001L, project, null);
        when(inviteRepository.findByToken("t-1")).thenReturn(Optional.of(invite));
        when(inviteRepository.findById(9001L)).thenReturn(Optional.of(invite));

        assertThat(context.inviteLockKey(null, "t-1")).isEqualTo("customer-invite:9001");
        assertThat(context.inviteLockKey(9001L, null)).isEqualTo("customer-invite:9001");
        assertThat(context.inviteLockKey(9001L, "t-1")).isEqualTo("customer-invite:9001");
    }

    @Test
    void acceptAndCancelShareTheSameAccessLock() {
        Project project = project(7001L);
        Credentials owner = credential(5001L);
        CustomerInvite accepted = invite(9001L, project, owner);
        when(inviteRepository.findById(9001L)).thenReturn(Optional.of(accepted));

        // 接受：身份来自登录凭证
        assertThat(context.inviteAccessLockKey(9001L, null, owner))
                .isEqualTo("customer-access:5001:7001");
        // 撤销：身份来自邀请的 acceptedBy，必须命中同一把锁
        assertThat(context.inviteAccessLockKey(9001L, null, null))
                .isEqualTo("customer-access:5001:7001");
    }

    @Test
    void unresolvedKeysStayNonBlankAndDoNotContend() {
        when(inviteRepository.findByToken("missing")).thenReturn(Optional.empty());
        when(inviteRepository.findById(123L)).thenReturn(Optional.of(invite(123L, project(7002L), null)));

        assertThat(context.inviteLockKey(123L, null)).isEqualTo("customer-invite:123");
        assertThat(context.inviteLockKey(null, "missing")).isEqualTo("customer-invite:unresolved:missing");
        assertThat(context.inviteAccessLockKey(null, "missing", null))
                .isEqualTo("customer-access:unresolved:missing");
    }

    private static Project project(Long projectId) {
        Project project = new Project();
        project.setProjectId(projectId);
        return project;
    }

    private static Credentials credential(Long creId) {
        Credentials credential = new Credentials();
        credential.setCreId(creId);
        return credential;
    }

    private static CustomerInvite invite(Long inviteId, Project project, Credentials acceptedBy) {
        CustomerInvite invite = new CustomerInvite();
        invite.setInviteId(inviteId);
        invite.setProject(project);
        invite.setAcceptedBy(acceptedBy);
        return invite;
    }
}
