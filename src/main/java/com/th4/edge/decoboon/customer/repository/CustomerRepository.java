package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 客户仓储。
 */
public interface CustomerRepository extends JpaRepository<Customer, Long>,
        JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByCustIdAndCompany_ComId(Long custId, Long comId);

    List<Customer> findByCompany_ComId(Long comId);

    long countByCompany_ComId(Long comId);

    List<Customer> findTop200ByCompany_ComIdOrderByCustIdDesc(Long comId);

    /** 设计师「我的客户」分页入口：查询参数与分页边界由调用方明确传入。 */
    default Page<Customer> findPageForDesigner(Specification<Customer> specification, Pageable pageable) {
        return findAll(specification, pageable);
    }

    /**
     * 设计师口径的客户阶段计数（本人负责 ∪ 指派给我设计）。
     * 走 SQL GROUP BY，返回行数 = 状态数，不做全量实体回捞（DEC-036 / BR-031）。
     */
    @Query("""
            select c.status, count(c)
            from Customer c
            where c.company.comId = :comId
              and (c.owner.empId = :empId or c.designer.empId = :empId)
            group by c.status
            """)
    List<Object[]> countStatusForDesigner(@Param("comId") Long comId, @Param("empId") Long empId);

    /** 设计师口径：跟进到期早于指定时刻的数量（逾期）。 */
    @Query("""
            select count(c)
            from Customer c
            where c.company.comId = :comId
              and (c.owner.empId = :empId or c.designer.empId = :empId)
              and c.status in :statuses
              and c.nextFollowAt is not null
              and c.nextFollowAt < :from
              and c.measureRequestedAt is null
              and c.pendingMeasureAt is null
              and c.measureAt is null
            """)
    long countFollowBeforeForDesigner(@Param("comId") Long comId,
                                      @Param("empId") Long empId,
                                      @Param("statuses") Collection<CustStatus> statuses,
                                      @Param("from") LocalDateTime from);

    /** 设计师口径：跟进到期落在 [from, to) 区间内的数量（今日跟进 / 3 天内）。 */
    @Query("""
            select count(c)
            from Customer c
            where c.company.comId = :comId
              and (c.owner.empId = :empId or c.designer.empId = :empId)
              and c.status in :statuses
              and c.nextFollowAt is not null
              and c.nextFollowAt >= :from
              and c.nextFollowAt < :to
              and c.measureRequestedAt is null
              and c.pendingMeasureAt is null
              and c.measureAt is null
            """)
    long countFollowBetweenForDesigner(@Param("comId") Long comId,
                                       @Param("empId") Long empId,
                                       @Param("statuses") Collection<CustStatus> statuses,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /** 设计师口径：待跟进但尚未安排下一次联系时间。 */
    @Query("""
            select count(c)
            from Customer c
            where c.company.comId = :comId
              and (c.owner.empId = :empId or c.designer.empId = :empId)
              and c.status in :statuses
              and c.nextFollowAt is null
              and c.measureRequestedAt is null
              and c.pendingMeasureAt is null
              and c.measureAt is null
            """)
    long countFollowUnplannedForDesigner(@Param("comId") Long comId,
                                         @Param("empId") Long empId,
                                         @Param("statuses") Collection<CustStatus> statuses);

    /** 设计师口径：待量房已约定时间但尚未指派设计师。 */
    @Query("""
            select count(c)
            from Customer c
            where c.company.comId = :comId
              and (c.owner.empId = :empId or c.designer.empId = :empId)
              and c.status = com.th4.edge.decoboon.customer.configure.CustStatus.PendingMeasure
              and c.measureAt is not null
              and c.designer is null
            """)
    long countPendingAssignForDesigner(@Param("comId") Long comId, @Param("empId") Long empId);

    /**
     * 设计师口径：最近一次推进早于阈值（沉睡风险）。
     * 等价于 max(上次跟进, 到访, 量房, 交接, 建档) &lt; threshold。
     * 注意 create_date_time 在库里是 epoch 毫秒（Long），必须用毫秒值比较。
     */
    @Query("""
            select count(c)
            from Customer c
            where c.company.comId = :comId
              and (c.owner.empId = :empId or c.designer.empId = :empId)
              and c.status in :statuses
              and (c.lastFollowAt is null or c.lastFollowAt < :threshold)
              and (c.lastVisitAt is null or c.lastVisitAt < :threshold)
              and (c.measureAt is null or c.measureAt < :threshold)
              and (c.handedOffAt is null or c.handedOffAt < :threshold)
              and (c.createTime.dateTime is null or c.createTime.dateTime < :thresholdMillis)
            """)
    long countIdleForDesigner(@Param("comId") Long comId,
                              @Param("empId") Long empId,
                              @Param("statuses") Collection<CustStatus> statuses,
                              @Param("threshold") LocalDateTime threshold,
                              @Param("thresholdMillis") Long thresholdMillis);

    List<Customer> findByCompany_ComIdAndOwner_EmpId(Long comId, Long empId);
    List<Customer> findByCompany_ComIdAndDesigner_EmpId(Long comId, Long empId);

    /** 已由分页查询限定租户与数据范围后，按主键批量装配当前页。 */
    List<Customer> findByCompany_ComIdAndCustIdIn(Long comId, java.util.Collection<Long> custIds);
    Optional<Customer> findByCustIdAndCompany_ComIdAndDesigner_EmpId(Long custId, Long comId, Long empId);

    List<Customer> findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(Long comId, Long storeId);

    List<Customer> findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(Long comId, Long teamId);


    /** 门店维度客户（`DEC-067`）：全部门店不过滤（含未归属门店客户），单店按客户 `store` 收窄。 */
    @EntityGraph(attributePaths = {"store", "owner", "designer"})
    @Query("select c from Customer c left join c.store cs where c.company.comId = :comId "
            + "and (:allStores = true or cs.storeId = :storeId)")
    List<Customer> findByCompanyStoreScope(@Param("comId") Long comId,
                                          @Param("allStores") boolean allStores,
                                          @Param("storeId") Long storeId);

    /** 未归属门店客户数（`DEC-067` BOSS-STORE-03：全部门店口径需显式标注）。 */
    long countByCompany_ComIdAndStoreIsNull(Long comId);

    /** 经营角色客户工作区：按门店范围聚合客户阶段，返回行数等于阶段数。 */
    @Query("""
            select c.status, count(c)
            from Customer c left join c.store store
            where c.company.comId = :comId
              and (:allStores = true or store.storeId = :storeId)
            group by c.status
            """)
    List<Object[]> countStatusByStoreScope(@Param("comId") Long comId,
                                          @Param("allStores") boolean allStores,
                                          @Param("storeId") Long storeId);

    /** 经营角色客户工作区：按负责人聚合，不在 Java 中回捞全部客户。 */
    @Query("""
            select owner.empId, owner.name, count(c),
                   sum(case when c.status = com.th4.edge.decoboon.customer.configure.CustStatus.PendingMeasure then 1 else 0 end),
                   sum(case when c.status in (com.th4.edge.decoboon.customer.configure.CustStatus.Signed,
                                              com.th4.edge.decoboon.customer.configure.CustStatus.Serving,
                                              com.th4.edge.decoboon.customer.configure.CustStatus.Finished)
                            then 1 else 0 end)
            from Customer c left join c.store store left join c.owner owner
            where c.company.comId = :comId
              and (:allStores = true or store.storeId = :storeId)
            group by owner.empId, owner.name
            """)
    List<Object[]> countOwnerGroupsByStoreScope(@Param("comId") Long comId,
                                               @Param("allStores") boolean allStores,
                                               @Param("storeId") Long storeId);

    /** 经营角色客户工作区：待跟进数由数据库按范围直接计数。 */
    @Query("""
            select count(c)
            from Customer c left join c.store store
            where c.company.comId = :comId
              and (:allStores = true or store.storeId = :storeId)
              and c.status in (com.th4.edge.decoboon.customer.configure.CustStatus.New,
                               com.th4.edge.decoboon.customer.configure.CustStatus.Following)
              and c.nextFollowAt is not null
              and c.nextFollowAt < :tomorrow
            """)
    long countFollowDueByStoreScope(@Param("comId") Long comId,
                                    @Param("allStores") boolean allStores,
                                    @Param("storeId") Long storeId,
                                    @Param("tomorrow") LocalDateTime tomorrow);

    /** 经营角色客户工作区：待指派设计师数量。 */
    @Query("""
            select count(c)
            from Customer c left join c.store store
            where c.company.comId = :comId
              and (:allStores = true or store.storeId = :storeId)
              and c.status = com.th4.edge.decoboon.customer.configure.CustStatus.PendingMeasure
              and c.measureAt is not null
              and c.designer is null
            """)
    long countPendingAssignByStoreScope(@Param("comId") Long comId,
                                        @Param("allStores") boolean allStores,
                                        @Param("storeId") Long storeId);
}
