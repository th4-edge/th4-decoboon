package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 收款仓储。 */
public interface ReceiptRepository extends JpaRepository<Receipt, Long>,
        JpaSpecificationExecutor<Receipt> {

    List<Receipt> findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(Long comId, Long projectId);

    @EntityGraph(attributePaths = {"project", "customer", "owner", "store", "team"})
    List<Receipt> findByCompany_ComIdOrderByReceivedAtDesc(Long comId);

    List<Receipt> findTop200ByCompany_ComIdOrderByReceivedAtDesc(Long comId);

    List<Receipt> findByCompany_ComIdAndProject_ProjectIdIn(Long comId, Collection<Long> projectIds);

    Optional<Receipt> findByCompany_ComIdAndIdempotencyKey(Long comId, String idempotencyKey);

    /** 分页后的批量详情读取：一次 JOIN FETCH 预取页面白名单关联，避免逐行懒加载。 */
    @Query("""
            select r from Receipt r
            left join fetch r.project p
            left join fetch p.property
            left join fetch p.customer
            left join fetch r.receivable rr
            left join fetch rr.property
            left join fetch r.customer
            left join fetch r.createdBy
            left join fetch r.reversedBy
            where r.receiptId in :ids
            """)
    List<Receipt> findDetailsByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"project", "receivable", "customer", "owner", "store", "team", "createdBy", "reversedBy"})
    Optional<Receipt> findByCompany_ComIdAndReceiptId(Long comId, Long receiptId);
}
