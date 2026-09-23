package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 合同变更单仓储。 */
public interface ContractChangeRepository extends JpaRepository<ContractChange, Long>,
        JpaSpecificationExecutor<ContractChange> {

    List<ContractChange> findByContractOrderByChangeIdDesc(ProjectContract contract);

    List<ContractChange> findByProject_ProjectIdAndCompany_ComIdOrderByChangeIdDesc(Long projectId, Long comId);

    List<ContractChange> findByCompany_ComIdOrderByChangeIdDesc(Long comId);

    List<ContractChange> findTop200ByCompany_ComIdOrderByChangeIdDesc(Long comId);
}
