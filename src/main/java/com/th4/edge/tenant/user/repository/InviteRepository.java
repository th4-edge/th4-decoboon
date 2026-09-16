package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.model.Invite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 员工邀请仓储。
 */
public interface InviteRepository extends JpaRepository<Invite, Long> {

    Invite findByInviteId(Long inviteId);

    boolean existsByTelNoAndStatus(String telNo, InviteStatus status);

    long countByCompany_ComIdAndStatus(Long comId, InviteStatus status);

    Page<Invite> findByCompany_ComId(Long comId, Pageable pageable);

    List<Invite> findByCompany_ComIdOrderByInviteIdDesc(Long comId);

    List<Invite> findByTelNoOrderByInviteIdDesc(String telNo);

    Invite findFirstByCompany_ComIdAndTelNoAndStatusOrderByInviteIdDesc(
            Long comId, String telNo, InviteStatus status);
}
