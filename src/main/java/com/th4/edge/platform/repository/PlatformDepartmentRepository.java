package com.th4.edge.platform.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.platform.model.PlatformDepartment;

import java.util.List;

/** 平台部门仓储。 */
public interface PlatformDepartmentRepository extends JpaRepository<PlatformDepartment, Long> {
    PlatformDepartment findByOrg_OrgIdAndCode(Long orgId, String code);
    List<PlatformDepartment> findByOrg_OrgIdOrderBySortAscDeptIdAsc(Long orgId);
}
