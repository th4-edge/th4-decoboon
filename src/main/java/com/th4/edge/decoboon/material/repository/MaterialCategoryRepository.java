package com.th4.edge.decoboon.material.repository;

import com.th4.edge.decoboon.material.model.MaterialCategory;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 主材分类仓储。 */
public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, Long>,
        JpaSpecificationExecutor<MaterialCategory> {

    List<MaterialCategory> findByCompany_ComIdOrderBySortAscMaterialCategoryIdAsc(Long comId);

    Optional<MaterialCategory> findFirstByCompany_ComIdAndNameIgnoreCase(Long comId, String name);

    Optional<MaterialCategory> findFirstByCompany_ComIdAndNameIgnoreCaseAndActiveTrue(Long comId, String name);
}
