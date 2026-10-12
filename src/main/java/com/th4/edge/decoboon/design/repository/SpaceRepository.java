package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 方案区域仓储。 */
public interface SpaceRepository extends JpaRepository<Space, Long>,
        JpaSpecificationExecutor<Space> {

    List<Space> findByDesign_DesignIdOrderBySortAsc(Long designId);

    java.util.Optional<Space> findBySpaceIdAndDesign_DesignId(Long spaceId, Long designId);

    List<Space> findByDesign_DesignIdIn(Collection<Long> designIds);

    /** 房屋区域联动：引用该房屋区域的全部方案区域。 */
    List<Space> findByHouseRegionId(Long houseRegionId);

    /** 房屋区域候选汇聚：该房屋全部方案区域。 */
    List<Space> findByProperty_PropertyId(Long propertyId);

    long deleteByDesign_DesignId(Long designId);
}
