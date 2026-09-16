package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 设计分项仓储。 */
public interface DesignItemRepository extends JpaRepository<DesignItem, Long>,
        JpaSpecificationExecutor<DesignItem> {

    List<DesignItem> findByDesign_DesignIdOrderBySortAsc(Long designId);

    List<DesignItem> findByDesign_DesignIdAndSpace_SpaceIdOrderBySortAsc(Long designId, Long spaceId);

    List<DesignItem> findByCompany_ComIdOrderByTypeAscCategoryAscNameAsc(Long comId);
}
