package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
            """)
    long countFollowBetweenForDesigner(@Param("comId") Long comId,
                                       @Param("empId") Long empId,
                                       @Param("statuses") Collection<CustStatus> statuses,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

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
    Optional<Customer> findByCustIdAndCompany_ComIdAndDesigner_EmpId(Long custId, Long comId, Long empId);

    List<Customer> findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(Long comId, Long storeId);

    List<Customer> findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(Long comId, Long teamId);
}
