package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 合同基线行仓储。 */
public interface ContractLineRepository extends JpaRepository<ContractLine, Long>,
        JpaSpecificationExecutor<ContractLine> {
    List<ContractLine> findByContractOrderBySortAsc(ProjectContract contract);
}
