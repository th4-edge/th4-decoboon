package com.th4.edge.decoboon.property.repository;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 房屋区域实测档案仓储（DEC-064 / OBJ-007）。 */
public interface PropertyRegionRepository extends JpaRepository<PropertyRegion, Long>,
        JpaSpecificationExecutor<PropertyRegion> {

    List<PropertyRegion> findByCompany_ComIdAndProperty_PropertyIdOrderBySortAscRegionIdAsc(Long comId, Long propertyId);

    List<PropertyRegion> findByCompany_ComIdAndProperty_PropertyIdInOrderBySortAscRegionIdAsc(
            Long comId, Collection<Long> propertyIds);

    Optional<PropertyRegion> findByCompany_ComIdAndRegionId(Long comId, Long regionId);

    /** 实例化继承：按区域键精确匹配。 */
    Optional<PropertyRegion> findFirstByCompany_ComIdAndProperty_PropertyIdAndRoomKey(
            Long comId, Long propertyId, String roomKey);

    /** 实例化继承兜底：按「区域类型 + 名称」匹配；歧义（多条）由调用方按待确认处理。 */
    List<PropertyRegion> findByCompany_ComIdAndProperty_PropertyIdAndRoomTypeAndName(
            Long comId, Long propertyId, SpaceType roomType, String name);

    List<PropertyRegion> findByCompany_ComIdAndProperty_PropertyIdAndRoomKey(Long comId, Long propertyId,
                                                                             String roomKey);

    long deleteByProperty_PropertyId(Long propertyId);
}
