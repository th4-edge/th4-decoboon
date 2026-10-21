package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 方案区域仓储。 */
public interface SpaceRepository extends JpaRepository<Space, Long>,
        JpaSpecificationExecutor<Space> {

    List<Space> findByDesign_DesignIdOrderBySortAsc(Long designId);

    java.util.Optional<Space> findBySpaceIdAndDesign_DesignId(Long spaceId, Long designId);

    List<Space> findByDesign_DesignIdIn(Collection<Long> designIds);

    @Query("select s.design.designId, count(s) from Space s "
            + "where s.design.designId in :designIds group by s.design.designId")
    List<Object[]> countByDesignIds(@Param("designIds") Collection<Long> designIds);


    long deleteByDesign_DesignId(Long designId);
}
