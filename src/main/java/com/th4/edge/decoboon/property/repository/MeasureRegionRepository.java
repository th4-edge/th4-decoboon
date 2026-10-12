package com.th4.edge.decoboon.property.repository;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 量房原始区域仓储（DEC-066 / OBJ-007）。 */
public interface MeasureRegionRepository extends JpaRepository<MeasureRegion, Long>,
        JpaSpecificationExecutor<MeasureRegion> {

    List<MeasureRegion> findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(Long comId, Long measureId);

    Optional<MeasureRegion> findByCompany_ComIdAndRegionId(Long comId, Long regionId);

    /** 实例化继承：按区域键精确匹配。 */
    Optional<MeasureRegion> findFirstByCompany_ComIdAndMeasure_MeasureIdAndRoomKey(Long comId, Long measureId,
                                                                                  String roomKey);

    /** 实例化继承兜底：按「区域类型 + 名称」匹配；多条由调用方按歧义处理（不静默择一）。 */
    List<MeasureRegion> findByCompany_ComIdAndMeasure_MeasureIdAndRoomTypeAndName(
            Long comId, Long measureId, SpaceType roomType, String name);

    long deleteByMeasure_MeasureId(Long measureId);
}
