package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.Subscription;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Collection;

/**
 * 企业订阅仓储。
 *
 * <p>非加锁读取；席位/门店额度校验（{@code subscription:{comId}}）与支付履约（{@code subscription:{comId}}）
 * 的并发串行由 {@code @DistributedLocked} 承担，替代原行级悲观锁。
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * 订阅校验在 Provider 事务之外执行（SubscriptionAccessRequestFilter → SubscriptionContext），
     * 生产关闭 Open Session In View，必须在此预取校验链路会用到的关联，
     * 否则访问 planVersion/plan 的懒加载代理会抛 LazyInitializationException。
     */
    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    Subscription findByCompany_ComId(Long comId);

    List<Subscription> findByPlan_PlanIdAndPlanVersionIsNull(Long planId);

    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    List<Subscription> findAllByOrderBySubIdDesc();

    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    List<Subscription> findByCompany_ComIdIn(Collection<Long> comIds);

    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    Subscription findBySubId(Long subId);
}
