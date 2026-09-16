package com.th4.edge.decoboon.property.repository;

import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 量房记录仓储。 */
public interface MeasureRepository extends JpaRepository<Measure, Long>,
        JpaSpecificationExecutor<Measure> {

    List<Measure> findByPropertyOrderByMeasureTimeDesc(com.th4.edge.decoboon.property.model.Property property);

    List<Measure> findByCompany_ComIdAndProperty_PropertyIdOrderByMeasureTimeDesc(Long comId, Long propertyId);
}
