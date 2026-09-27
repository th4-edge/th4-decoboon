package com.th4.edge.platform.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.platform.model.PlatformOnboardingServiceRecord;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 平台企业入驻服务记录仓储。 */
public interface PlatformOnboardingServiceRepository extends JpaRepository<PlatformOnboardingServiceRecord, Long> {
    List<PlatformOnboardingServiceRecord> findByOrg_OrgIdAndTargetComIdInOrderByServiceIdDesc(
            Long orgId, Collection<Long> targetComIds);

    List<PlatformOnboardingServiceRecord> findByOrg_OrgIdAndTargetComIdOrderByServiceIdDesc(
            Long orgId, Long targetComId);

    Optional<PlatformOnboardingServiceRecord> findFirstByOrg_OrgIdAndTargetComIdOrderByServiceIdDesc(
            Long orgId, Long targetComId);
}
