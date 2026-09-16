package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.user.configure.MemberLogAction;
import com.th4.edge.tenant.user.model.CompanyMemberLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 成员变更日志仓储。 */
public interface CompanyMemberLogRepository extends JpaRepository<CompanyMemberLog, Long> {

    Page<CompanyMemberLog> findByCompany_ComIdOrderByCreateTime_DateTimeDesc(Long comId, Pageable pageable);

    @Query("select log from CompanyMemberLog log where log.user.creId = :userId "
            + "and log.action = :action and log.createTime.dateTime > :after")
    List<CompanyMemberLog> findRecentLogs(@Param("userId") Long userId,
                                         @Param("action") MemberLogAction action,
                                         @Param("after") Long afterEpochMilli);

    @Query("select count(log) from CompanyMemberLog log where log.user.creId = :userId "
            + "and log.action = :action and log.createTime.dateTime > :after")
    long countRecentLogs(@Param("userId") Long userId,
                         @Param("action") MemberLogAction action,
                         @Param("after") Long afterEpochMilli);
}
