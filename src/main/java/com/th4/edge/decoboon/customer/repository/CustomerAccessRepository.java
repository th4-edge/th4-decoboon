package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

/** 业主项目授权仓储。 */
public interface CustomerAccessRepository extends JpaRepository<CustomerAccess, Long> {

    @EntityGraph(attributePaths = {"project", "project.property", "project.customer", "project.designer",
            "project.manager", "project.support", "customer"})
    List<CustomerAccess> findByCredential_CreIdAndStatusOrderByAccessIdAsc(
            Long creId, CustomerAccessStatus status);

    @EntityGraph(attributePaths = {"project", "project.property", "project.customer", "project.designer",
            "project.manager", "project.support", "customer"})
    Optional<CustomerAccess> findByCredential_CreIdAndProject_ProjectIdAndStatus(
            Long creId, Long projectId, CustomerAccessStatus status);

    Optional<CustomerAccess> findByCredential_CreIdAndProject_ProjectId(Long creId, Long projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerAccess> findLockedByCredential_CreIdAndProject_ProjectId(Long creId, Long projectId);

    List<CustomerAccess> findByInvite_InviteId(Long inviteId);

    /** 按项目查有效业主授权，用于向业主侧发送订阅消息。 */
    List<CustomerAccess> findByProject_ProjectIdAndStatus(Long projectId, CustomerAccessStatus status);
}
