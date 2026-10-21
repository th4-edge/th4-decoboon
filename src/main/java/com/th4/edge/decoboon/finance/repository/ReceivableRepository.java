package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 应收计划仓储。 */
public interface ReceivableRepository extends JpaRepository<Receivable, Long>,
        JpaSpecificationExecutor<Receivable> {

    List<Receivable> findByProjectOrderByDueDateAsc(Project project);

    List<Receivable> findByCompany_ComIdAndProject_ProjectIdOrderByDueDateAsc(Long comId, Long projectId);

    @EntityGraph(attributePaths = {"project", "customer", "property", "store", "owner", "contract"})
    List<Receivable> findByCompany_ComIdOrderByDueDateAsc(Long comId);

    /** 企业负责人增减项工作台：按项目集合批量取应收，用于变更款落地核对。 */
    @EntityGraph(attributePaths = {"project", "customer", "property", "store", "owner", "contract"})
    List<Receivable> findByProject_ProjectIdInOrderByDueDateAsc(Collection<Long> projectIds);

    /** 分页后的批量详情读取：一次 JOIN FETCH 预取页面白名单关联，避免逐行懒加载。 */
    @Query("""
            select r from Receivable r
            left join fetch r.project p
            left join fetch p.property
            left join fetch p.customer
            left join fetch p.manager
            left join fetch r.property
            left join fetch r.customer
            left join fetch r.createdBy
            where r.receivableId in :ids
            """)
    List<Receivable> findDetailsByIdIn(@Param("ids") Collection<Long> ids);
}
