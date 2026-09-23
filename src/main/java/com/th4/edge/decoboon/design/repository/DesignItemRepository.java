package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 设计分项仓储。 */
public interface DesignItemRepository extends JpaRepository<DesignItem, Long>,
        JpaSpecificationExecutor<DesignItem> {

    /**
     * 带上材料库主数据：业主旅程/谈单投影会读取 catalogItem.craft（施工工艺）。
     * ManyToOne 预取不会放大结果行；显式预取保证关闭 OSIV 时也不会触发逐条懒加载。
     */
    @EntityGraph(attributePaths = {"catalogItem"})
    List<DesignItem> findByDesign_DesignIdOrderBySortAsc(Long designId);

    List<DesignItem> findByDesign_DesignIdIn(Collection<Long> designIds);

    List<DesignItem> findByDesign_DesignIdAndSpace_SpaceIdOrderBySortAsc(Long designId, Long spaceId);

    List<DesignItem> findByCompany_ComIdOrderByTypeAscCategoryAscNameAsc(Long comId);
}
