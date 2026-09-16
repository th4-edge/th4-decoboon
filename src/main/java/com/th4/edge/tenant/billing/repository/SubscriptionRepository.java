package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

/**
 * 企业订阅仓储。
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * 订阅校验在 Provider 事务之外执行（SubscriptionAccessRequestFilter → SubscriptionContext），
     * 生产关闭 Open Session In View，必须在此预取校验链路会用到的关联，
     * 否则访问 planVersion/plan 的懒加载代理会抛 LazyInitializationException。
     */
    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    Subscription findByCompany_ComId(Long comId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subscription s where s.company.comId = :comId")
    Subscription findByCompany_ComIdForUpdate(@Param("comId") Long comId);

    List<Subscription> findByPlan_PlanIdAndPlanVersionIsNull(Long planId);

    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    List<Subscription> findAllByOrderBySubIdDesc();

    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    List<Subscription> findByCompany_ComIdIn(Collection<Long> comIds);

    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    Subscription findBySubId(Long subId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
    @Query("select s from Subscription s where s.subId = :subId")
    Subscription findBySubIdForUpdate(@Param("subId") Long subId);
}
