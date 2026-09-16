package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 应收计划仓储。 */
public interface ReceivableRepository extends JpaRepository<Receivable, Long>,
        JpaSpecificationExecutor<Receivable> {

    List<Receivable> findByProjectOrderByDueDateAsc(Project project);

    List<Receivable> findByCompany_ComIdAndProject_ProjectIdOrderByDueDateAsc(Long comId, Long projectId);

    List<Receivable> findByCompany_ComIdOrderByDueDateAsc(Long comId);
}
