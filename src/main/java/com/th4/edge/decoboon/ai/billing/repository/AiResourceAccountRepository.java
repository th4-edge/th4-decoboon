package com.th4.edge.decoboon.ai.billing.repository;

import com.th4.edge.decoboon.ai.billing.model.AiResourceAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * AI 资源账户仓储。
 *
 * <p>非加锁读取；账户余额的先读后写由 {@code @DistributedLocked} 按企业串行化。</p>
 */
public interface AiResourceAccountRepository extends JpaRepository<AiResourceAccount, Long> {

    Optional<AiResourceAccount> findByCompany_ComId(Long comId);
}
