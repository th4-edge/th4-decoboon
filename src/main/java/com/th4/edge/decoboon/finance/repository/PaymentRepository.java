package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 付款仓储。 */
public interface PaymentRepository extends JpaRepository<Payment, Long>,
        JpaSpecificationExecutor<Payment> {

    List<Payment> findByCompany_ComIdAndProject_ProjectIdOrderByPaidAtDesc(Long comId, Long projectId);

    @EntityGraph(attributePaths = {"project", "payable", "partner", "worker", "owner", "store", "team"})
    List<Payment> findByCompany_ComIdOrderByPaidAtDesc(Long comId);

    List<Payment> findTop200ByCompany_ComIdOrderByPaidAtDesc(Long comId);

    List<Payment> findByCompany_ComIdAndProject_ProjectIdIn(Long comId, Collection<Long> projectIds);

    Optional<Payment> findByCompany_ComIdAndIdempotencyKey(Long comId, String idempotencyKey);

    /** 分页后的批量详情读取：一次 JOIN FETCH 预取页面白名单关联，避免逐行懒加载。 */
    @Query("""
            select p from Payment p
            left join fetch p.project pr
            left join fetch pr.property
            left join fetch pr.customer
            left join fetch pr.manager
            left join fetch p.payable pa
            left join fetch pa.property
            left join fetch p.worker
            left join fetch p.createdBy
            where p.paymentId in :ids
            """)
    List<Payment> findDetailsByIdIn(@Param("ids") Collection<Long> ids);
}
