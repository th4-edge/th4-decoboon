package com.th4.edge.decoboon.ai.billing.repository;

import com.th4.edge.decoboon.ai.billing.model.AiResourceAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiResourceAccountRepository extends JpaRepository<AiResourceAccount, Long> {
    Optional<AiResourceAccount> findByCompany_ComId(Long comId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AiResourceAccount account where account.company.comId = :comId")
    Optional<AiResourceAccount> lockByCompanyId(@Param("comId") Long comId);
}
