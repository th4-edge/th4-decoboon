package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.user.model.UserSecurityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 用户安全日志仓储。 */
public interface UserSecurityLogRepository extends JpaRepository<UserSecurityLog, Long> {

    Page<UserSecurityLog> findByUserIdOrderByCreateTime_DateTimeDesc(Long userId, Pageable pageable);

    Page<UserSecurityLog> findByPhoneOrderByCreateTime_DateTimeDesc(String phone, Pageable pageable);
}
