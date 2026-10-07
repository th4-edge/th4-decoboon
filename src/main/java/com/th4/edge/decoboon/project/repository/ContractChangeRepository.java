package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 合同变更单仓储。 */
public interface ContractChangeRepository extends JpaRepository<ContractChange, Long>,
        JpaSpecificationExecutor<ContractChange> {

    List<ContractChange> findByContractOrderByChangeIdDesc(ProjectContract contract);

    List<ContractChange> findByProject_ProjectIdAndCompany_ComIdOrderByChangeIdDesc(Long projectId, Long comId);

    List<ContractChange> findByCompany_ComIdOrderByChangeIdDesc(Long comId);

    List<ContractChange> findTop200ByCompany_ComIdOrderByChangeIdDesc(Long comId);

    /** 业主同频页批量取签认证据（已按项目/企业范围收敛后调用，避免逐单 N+1）。 */
    @Query(value = "select change_id, url from db_contract_change_image where change_id in (:changeIds)",
            nativeQuery = true)
    List<Object[]> findImageRowsByChangeIds(@Param("changeIds") Collection<Long> changeIds);
}
