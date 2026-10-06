package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
