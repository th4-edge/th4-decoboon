package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.PlanAddon;

import java.util.List;

/**
 * 套餐加购商品仓储。
 */
public interface PlanAddonRepository extends JpaRepository<PlanAddon, Long> {

    PlanAddon findByCode(String code);

    List<PlanAddon> findByEnabledTrueOrderByPriorityAsc();
}
