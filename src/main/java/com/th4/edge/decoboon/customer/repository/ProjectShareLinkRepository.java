package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.ProjectShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 项目分享链接仓储。 */
public interface ProjectShareLinkRepository extends JpaRepository<ProjectShareLink, Long> {

    Optional<ProjectShareLink> findByToken(String token);

    long countByProject_ProjectIdAndCompany_ComIdAndStatus(Long projectId, Long comId,
                                                          com.th4.edge.decoboon.customer.configure.ShareStatus status);
}
