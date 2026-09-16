package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

/** 业主邀请仓储。 */
public interface CustomerInviteRepository extends JpaRepository<CustomerInvite, Long> {

    Optional<CustomerInvite> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerInvite> findLockedByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerInvite> findLockedByInviteId(Long inviteId);

    List<CustomerInvite> findByProject_ProjectIdOrderByInviteIdDesc(Long projectId);
}
