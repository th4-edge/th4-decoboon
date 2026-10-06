package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 业主邀请仓储。
 *
 * <p>非加锁读取；邀请的接受一次性与接受/撤销并发由 {@code @DistributedLocked} 串行化。</p>
 */
public interface CustomerInviteRepository extends JpaRepository<CustomerInvite, Long> {

    Optional<CustomerInvite> findByToken(String token);

    List<CustomerInvite> findByProject_ProjectIdOrderByInviteIdDesc(Long projectId);
}
