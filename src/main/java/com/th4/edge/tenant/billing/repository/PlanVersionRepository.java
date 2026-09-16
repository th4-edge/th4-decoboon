package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.billing.model.PlanVersion;

import java.util.List;

/**
 * 套餐版本仓储。
 */
public interface PlanVersionRepository extends JpaRepository<PlanVersion, Long> {

    PlanVersion findTopByPlan_PlanIdOrderByVersionNoDesc(Long planId);

    PlanVersion findByPlan_PlanIdAndVersionNo(Long planId, Integer versionNo);

    List<PlanVersion> findByPlan_PlanIdOrderByVersionNoDesc(Long planId);

    long countByPlan_PlanId(Long planId);
}
