package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /** 指定客户是否存在不在终态集合内的正式合同。 */
    boolean existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
            Long comId, Long customerId, Collection<ContractStatus> statuses);

    boolean existsByProject_ProjectId(Long projectId);

    boolean existsByProperty_PropertyId(Long propertyId);

    @EntityGraph(attributePaths = {"project", "customer", "property", "store", "owner", "quote"})
    List<ProjectContract> findByCompany_ComIdOrderByContractIdDesc(Long comId);

    /** 按客户集合取合同（用于客户经营投影的最新合同，量级 = 当前页客户数）。 */
    List<ProjectContract> findByCompany_ComIdAndCustomer_CustIdInOrderByContractIdDesc(
            Long comId, java.util.Collection<Long> custIds);

    long countByCompany_ComId(Long comId);

    List<ProjectContract> findTop200ByCompany_ComIdOrderByContractIdDesc(Long comId);
}
