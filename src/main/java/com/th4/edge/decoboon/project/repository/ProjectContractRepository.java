package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 项目合同基线仓储。 */
public interface ProjectContractRepository extends JpaRepository<ProjectContract, Long>,
        JpaSpecificationExecutor<ProjectContract> {

    Optional<ProjectContract> findByProject(Project project);

    Optional<ProjectContract> findByQuote_QuoteId(Long quoteId);

    List<ProjectContract> findByQuote_QuoteIdIn(Collection<Long> quoteIds);

    Optional<ProjectContract> findByProject_ProjectIdAndCompany_ComId(Long projectId, Long comId);

    Optional<ProjectContract> findByContractIdAndCompany_ComId(Long contractId, Long comId);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    boolean existsByProject_ProjectId(Long projectId);

    boolean existsByProperty_PropertyId(Long propertyId);

    List<ProjectContract> findByCompany_ComIdOrderByContractIdDesc(Long comId);

    long countByCompany_ComId(Long comId);

    List<ProjectContract> findTop200ByCompany_ComIdOrderByContractIdDesc(Long comId);
}
