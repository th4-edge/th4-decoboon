package com.th4.edge.decoboon.customer.context;

import com.th4.edge.components.lock.DistributedLocked;
import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * 业主邀请查询、一次性接受和撤销上下文。
 *
 * <p>并发口径（2026-10-07 锁迁移 Batch 5）：邀请「接受」先按邀请串行（保证一次性、以及与撤销互斥），
 * 再按「微信身份 + 项目」授权串行（保证 cust_customer_access 的 (cre_id, project_id) 唯一授权不被并发重复插入）；
 * 两个锁都在事务外层获取，因此事务内的读取不会读到加锁前的旧快照。</p>
 */
@Component
public class CustomerInviteContext {

    private final CustomerInviteRepository inviteRepository;
    private final CustomerAccessRepository accessRepository;
    private final UserIdentityContext userContext;
    private final ProjectAccessContext projectAccessContext;

    public CustomerInviteContext(CustomerInviteRepository inviteRepository,
                                 CustomerAccessRepository accessRepository,
                                 UserIdentityContext userContext,
                                 ProjectAccessContext projectAccessContext) {
        this.inviteRepository = inviteRepository;
        this.accessRepository = accessRepository;
        this.userContext = userContext;
        this.projectAccessContext = projectAccessContext;
    }

    @Transactional
    public CustomerInvite requireInfo(Long inviteId, String token, String phone) {
        CustomerInvite invite = find(inviteId, token);
        if (token != null && !token.isBlank() && inviteId != null
                && !Objects.equals(invite.getInviteId(), inviteId)) {
            throw unavailable();
        }
        if (phone != null && !phone.isBlank() && !samePhone(invite.getTelNo(), phone)) {
            throw unavailable();
        }
        if (phone == null || phone.isBlank()) {
            if (token == null || token.isBlank()) {
                throw unavailable();
            }
        }
        expireIfNeeded(invite);
        invite.setMaskedTelNo(mask(invite.getTelNo()));
        return invite;
    }

    @DistributedLocked(key = "#root.inviteLockKey(#inviteId, #token)",
            extraKeys = {"#root.inviteAccessLockKey(#inviteId, #token, #principal)"},
            message = "该邀请正在处理中，请稍后重试")
    @Transactional
    public CustomerInvite accept(Long inviteId, String token, Credentials principal) {
        Credentials credential = userContext.lock(principal.getCreId());
        CustomerInvite invite = find(inviteId, token);
        if (token != null && !token.isBlank() && inviteId != null
                && !Objects.equals(invite.getInviteId(), inviteId)) {
            throw unavailable();
        }
        if (invite.getStatus() != InviteStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", "邀请已失效"));
        }
        expireIfNeeded(invite);
        if (invite.getStatus() == InviteStatus.Expired) {
            throw new DefinedException(new DefaultErrorCode("400", "邀请已过期"));
        }
        if (!samePhone(invite.getTelNo(), credential.getPhone())) {
            throw new DefinedException(new DefaultErrorCode("400", "授权手机号与邀请手机号不一致"));
        }

        Project project = invite.getProject();
        CustomerAccess access = accessRepository
                .findByCredential_CreIdAndProject_ProjectId(credential.getCreId(), project.getProjectId())
                .orElseGet(CustomerAccess::new);
        access.setCompany(project.getCompany());
        access.setCredential(credential);
        access.setCustomer(invite.getCustomer() == null ? project.getCustomer() : invite.getCustomer());
        access.setProject(project);
        access.setInvite(invite);
        access.setDisplayName(invite.getCustomerName());
        access.setStatus(CustomerAccessStatus.Active);
        access.setGrantedAt(LocalDateTime.now());
        access.setRevokedAt(null);
        access.setRevokedBy(null);
        accessRepository.save(access);

        invite.setAcceptedBy(credential);
        invite.setAcceptedAt(LocalDateTime.now());
        invite.setStatus(InviteStatus.Accepted);
        inviteRepository.save(invite);
        return invite;
    }

    @DistributedLocked(key = "#root.inviteLockKey(#inviteId, null)",
            extraKeys = {"#root.inviteAccessLockKey(#inviteId, null, null)"},
            message = "该邀请正在处理中，请稍后重试")
    @Transactional
    public CustomerInvite cancel(Employee operator, Long inviteId) {
        CustomerInvite candidate = inviteRepository.findById(inviteId)
                .orElseThrow(CustomerInviteContext::unavailable);
        projectAccessContext.requireVisible(candidate.getProject(), operator);
        CustomerInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(CustomerInviteContext::unavailable);
        projectAccessContext.requireVisible(invite.getProject(), operator);

        if (invite.getStatus() == InviteStatus.Cancelled) {
            return invite;
        }
        if (invite.getStatus() != InviteStatus.Pending && invite.getStatus() != InviteStatus.Accepted) {
            throw new DefinedException(new DefaultErrorCode("400", "邀请当前状态不可撤销"));
        }
        var accesses = new LinkedHashSet<CustomerAccess>();
        accesses.addAll(accessRepository.findByInvite_InviteId(invite.getInviteId()));
        if (invite.getAcceptedBy() != null) {
            accessRepository.findByCredential_CreIdAndProject_ProjectId(
                            invite.getAcceptedBy().getCreId(), invite.getProject().getProjectId())
                    .filter(access -> access.getStatus() == CustomerAccessStatus.Active)
                    .ifPresent(accesses::add);
        }
        for (CustomerAccess access : accesses) {
            if (access.getStatus() == CustomerAccessStatus.Active) {
                access.setStatus(CustomerAccessStatus.Revoked);
                access.setRevokedAt(LocalDateTime.now());
                access.setRevokedBy(operator);
                accessRepository.save(access);
            }
        }
        invite.setStatus(InviteStatus.Cancelled);
        invite.setCancelledAt(LocalDateTime.now());
        invite.setCancelledBy(operator);
        return inviteRepository.save(invite);
    }

    /**
     * 邀请维度锁键：按邀请 ID 串行；未解析到邀请时退化为 token/参数维度，保证键非空。
     */
    public String inviteLockKey(Long inviteId, String token) {
        Long resolved = resolveInviteId(inviteId, token);
        if (resolved != null) {
            return "customer-invite:" + resolved;
        }
        return "customer-invite:unresolved:" + (inviteId != null ? inviteId : token);
    }

    /**
     * 业主授权维度锁键：按「微信身份 + 项目」串行（对应 cust_customer_access 的 (cre_id, project_id) 唯一约束）。
     *
     * <p>接受邀请时身份来自登录凭证，撤销邀请时身份来自邀请的 acceptedBy；两者都解析不到时使用不与他人争用的键，
     * 保证锁键非空且不引入额外互斥。</p>
     */
    public String inviteAccessLockKey(Long inviteId, String token, Credentials principal) {
        CustomerInvite invite = resolveInvite(inviteId, token);
        Long creId = principal != null
                ? principal.getCreId()
                : (invite == null || invite.getAcceptedBy() == null ? null : invite.getAcceptedBy().getCreId());
        Long projectId = invite == null || invite.getProject() == null ? null : invite.getProject().getProjectId();
        if (creId != null && projectId != null) {
            return "customer-access:" + creId + ":" + projectId;
        }
        return "customer-access:unresolved:" + (inviteId != null ? inviteId : token);
    }

    private Long resolveInviteId(Long inviteId, String token) {
        CustomerInvite invite = resolveInvite(inviteId, token);
        return invite == null ? null : invite.getInviteId();
    }

    private CustomerInvite resolveInvite(Long inviteId, String token) {
        if (token != null && !token.isBlank()) {
            return inviteRepository.findByToken(token).orElse(null);
        }
        if (inviteId != null) {
            return inviteRepository.findById(inviteId).orElse(null);
        }
        return null;
    }

    private CustomerInvite find(Long inviteId, String token) {
        if (token != null && !token.isBlank()) {
            return inviteRepository.findByToken(token).orElseThrow(CustomerInviteContext::unavailable);
        }
        if (inviteId != null) {
            return inviteRepository.findById(inviteId).orElseThrow(CustomerInviteContext::unavailable);
        }
        throw unavailable();
    }

    private void expireIfNeeded(CustomerInvite invite) {
        if (invite.getStatus() == InviteStatus.Pending && invite.getExpireTime() != null
                && invite.getExpireTime().isBefore(LocalDateTime.now())) {
            invite.setStatus(InviteStatus.Expired);
            inviteRepository.save(invite);
        }
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String value = phone.trim().replace(" ", "").replace("-", "");
        if (value.startsWith("+86") && value.length() > 3) {
            return value.substring(3);
        }
        return value;
    }

    private boolean samePhone(String expected, String actual) {
        String left = normalizePhone(expected);
        String right = normalizePhone(actual);
        return left != null && !left.isBlank() && left.equals(right);
    }

    private static String mask(String phone) {
        String value = normalizePhone(phone);
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() < 7) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private static DefinedException unavailable() {
        return new DefinedException(new DefaultErrorCode("404", "邀请不存在或不可用"));
    }
}
