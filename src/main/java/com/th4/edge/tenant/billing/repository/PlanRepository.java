package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.Plan;

import java.util.List;

/**
 * 套餐仓储。
 */
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Plan findByCode(String code);

    List<Plan> findByEnabledTrueAndOneTimeOnlyFalseOrderByPriorityAsc();

    List<Plan> findByEnabledTrueAndOneTimeOnlyFalseAndPriceGreaterThanOrderByPriorityAsc(Long price);
}
