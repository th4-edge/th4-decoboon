package com.th4.edge.decoboon.material.repository;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 主材下单作业单仓储。 */
public interface MaterialOrderSheetRepository extends JpaRepository<MaterialOrderSheet, Long>,
        JpaSpecificationExecutor<MaterialOrderSheet> {

    List<MaterialOrderSheet> findByProject_ProjectIdOrderBySheetIdDesc(Long projectId);

    List<MaterialOrderSheet> findByProject_ProjectIdInOrderBySheetIdDesc(Collection<Long> projectIds);

    List<MaterialOrderSheet> findByCompany_ComIdOrderBySheetIdDesc(Long comId);

    Optional<MaterialOrderSheet> findFirstByCompany_ComIdAndSheetNo(Long comId, String sheetNo);

    Optional<MaterialOrderSheet> findFirstBySheetIdAndCompany_ComId(Long sheetId, Long comId);

    boolean existsByCompany_ComIdAndSheetNo(Long comId, String sheetNo);

    long countByProject_ProjectIdAndStatusIn(Long projectId, Collection<com.th4.edge.decoboon.material.configure.MaterialSheetStatus> statuses);
}
