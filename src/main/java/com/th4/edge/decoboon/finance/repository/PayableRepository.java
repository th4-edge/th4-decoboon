package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 应付仓储。 */
public interface PayableRepository extends JpaRepository<Payable, Long>,
        JpaSpecificationExecutor<Payable> {

    List<Payable> findByProjectOrderByDueDateAsc(Project project);

    List<Payable> findByCompany_ComIdAndProject_ProjectIdOrderByDueDateAsc(Long comId, Long projectId);

    List<Payable> findByCompany_ComIdOrderByDueDateAsc(Long comId);

    List<Payable> findByCompany_ComIdAndPayeePartner_PartnerIdOrderByDueDateAsc(Long comId, Long partnerId);
}
