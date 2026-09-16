package com.th4.edge.platform.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.platform.model.PlatformOrg;

/** 平台组织仓储。 */
public interface PlatformOrgRepository extends JpaRepository<PlatformOrg, Long> {
    PlatformOrg findByCode(String code);
    PlatformOrg findByOrgId(Long orgId);
}
