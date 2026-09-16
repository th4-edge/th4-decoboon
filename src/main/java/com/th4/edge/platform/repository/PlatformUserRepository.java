package com.th4.edge.platform.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.platform.model.PlatformUser;
import org.springframework.data.jpa.repository.EntityGraph;

/** 平台用户仓储。 */
public interface PlatformUserRepository extends JpaRepository<PlatformUser, Long> {
    PlatformUser findByPhone(String phone);

    @EntityGraph(attributePaths = {"org", "department", "department.parent"})
    PlatformUser findByUserId(Long userId);

    @EntityGraph(attributePaths = {"org", "department", "department.parent"})
    PlatformUser findWithOrgAndDepartmentByUserId(Long userId);
}
