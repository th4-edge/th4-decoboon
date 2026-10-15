package com.th4.edge.decoboon.customer.context;

import com.th4.edge.components.lock.DistributedLocked;
import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.configure.SessionType;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 业主身份与项目显式授权上下文。 */
@Component
public class CustomerAccessContext {

    private final CustomerAccessRepository accessRepository;
    private final UserIdentityContext userContext;

    public CustomerAccessContext(CustomerAccessRepository accessRepository,
                                 UserIdentityContext userContext) {
        this.accessRepository = accessRepository;
        this.userContext = userContext;
    }

    /** 业主接口只接受微信 Credentials 会话，不把员工 token 降级或提升为业主身份。 */
    public Credentials requireCredential(User<?> principal) {
        if (!(principal instanceof Credentials credentials)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前会话不是业主身份"));
        }
        return userContext.requireUser(credentials.getCreId());
    }

    public List<CustomerAccess> activeAccesses(Credentials credentials) {
        return accessRepository.findByCredential_CreIdAndStatusOrderByAccessIdAsc(
                credentials.getCreId(), CustomerAccessStatus.Active);
    }

    public CustomerAccess requireAccess(Credentials credentials, Long projectId) {
        if (projectId == null) {
            throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
        }
        return accessRepository.findByCredential_CreIdAndProject_ProjectIdAndStatus(
                        credentials.getCreId(), projectId, CustomerAccessStatus.Active)
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "项目不存在或无权访问")));
    }

    public Project requireProject(Credentials credentials, Long projectId) {
        return requireAccess(credentials, projectId).getProject();
    }

    /**
     * 扫码开通业主授权（项目码 + 授权手机号 == 房屋所属客户档案手机号）。
     *
     * <p>并发安全：与邀请接受路径（`CustomerInviteContext.accept`）使用**同一锁键**
     * `customer-access:{creId}:{projectId}`，保证 `cust_customer_access` 的
     * (cre_id, project_id) 唯一授权不会并发重复插入。</p>
     *
     * <p>授权来源 `invite` 为空——扫码进入不以邀请为前提（对齐「以房屋所属客户开通业主端」口径）；
     * 重复扫码为幂等更新，不新增第二条授权。</p>
     */
    @DistributedLocked(key = "#root.accessLockKey(#credential.creId, #project.projectId)",
            message = "该项目授权正在处理中，请稍后重试")
    @Transactional
    public CustomerAccess grantByScan(Credentials credential, Project project, Customer customer) {
        if (credential == null || credential.getCreId() == null) {
            throw new DefinedException(new DefaultErrorCode("401", "当前用户不存在"));
        }
        if (project == null || project.getProjectId() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
        }
        CustomerAccess access = accessRepository
                .findByCredential_CreIdAndProject_ProjectId(credential.getCreId(), project.getProjectId())
                .orElseGet(CustomerAccess::new);
        access.setCompany(project.getCompany());
        access.setCredential(credential);
        access.setCustomer(customer == null ? project.getCustomer() : customer);
        access.setProject(project);
        access.setInvite(null);
        access.setDisplayName(pickDisplayName(customer, project));
        access.setStatus(CustomerAccessStatus.Active);
        if (access.getGrantedAt() == null) {
            access.setGrantedAt(LocalDateTime.now());
        }
        access.setRevokedAt(null);
        access.setRevokedBy(null);
        return accessRepository.save(access);
    }

    /** 与邀请接受路径保持同一锁键前缀，避免两条路径并发写同一 (cre_id, project_id)。 */
    public String accessLockKey(Long creId, Long projectId) {
        return "customer-access:" + creId + ":" + projectId;
    }

    private String pickDisplayName(Customer customer, Project project) {
        Customer target = customer == null ? project.getCustomer() : customer;
        String name = target == null ? null : target.getName();
        if (name == null || name.isBlank()) {
            name = "业主";
        }
        // displayName 列为 varchar(32)
        return name.length() > 32 ? name.substring(0, 32) : name;
    }

    public void attachSession(Credentials credentials) {
        var employee = userContext.activeEmployee(credentials);
        if (employee != null) {
            credentials.setEmployee(employee);
            credentials.setSessionType(SessionType.Employee);
            credentials.setCustomerAccesses(activeAccesses(credentials));
            return;
        }
        List<CustomerAccess> accesses = activeAccesses(credentials);
        if (!accesses.isEmpty()) {
            credentials.setSessionType(SessionType.Customer);
            credentials.setCustomerAccesses(accesses);
            return;
        }
        credentials.setSessionType(SessionType.User);
        credentials.setCustomerAccesses(List.of());
    }
}
