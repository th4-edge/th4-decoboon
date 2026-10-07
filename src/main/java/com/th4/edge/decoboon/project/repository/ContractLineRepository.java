package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 合同基线行仓储。 */
public interface ContractLineRepository extends JpaRepository<ContractLine, Long>,
        JpaSpecificationExecutor<ContractLine> {
    List<ContractLine> findByContractOrderBySortAsc(ProjectContract contract);

    /** 合同变更批量录入：一次按合同范围加载原合同行，避免逐行查库。 */
    List<ContractLine> findByLineIdInAndContract_ContractId(java.util.Collection<Long> lineIds, Long contractId);

    @EntityGraph(attributePaths = {"sourceQuoteLine", "sourceQuoteLine.catalogItem"})
    List<ContractLine> findByContract_Project_ProjectIdOrderBySortAsc(Long projectId);

    /** 财务汇总：一次取多项目合同行（预取合同所属项目便于分组 + 报价行/目录项）。 */
    @EntityGraph(attributePaths = {"contract", "contract.project", "sourceQuoteLine", "sourceQuoteLine.catalogItem"})
    List<ContractLine> findByContract_Project_ProjectIdInOrderBySortAsc(java.util.Collection<Long> projectIds);
}
