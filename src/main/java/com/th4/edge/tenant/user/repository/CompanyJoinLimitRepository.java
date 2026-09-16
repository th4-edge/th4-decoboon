package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.user.model.CompanyJoinLimit;

import java.util.Optional;

/** 企业加入限制仓储。 */
public interface CompanyJoinLimitRepository extends JpaRepository<CompanyJoinLimit, Long> {

    Optional<CompanyJoinLimit> findByUser_CreId(Long userId);

    Optional<CompanyJoinLimit> findByPhone(String phone);
}
