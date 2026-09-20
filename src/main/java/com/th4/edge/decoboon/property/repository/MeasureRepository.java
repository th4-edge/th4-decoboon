package com.th4.edge.decoboon.property.repository;

import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 量房记录仓储。 */
public interface MeasureRepository extends JpaRepository<Measure, Long>,
        JpaSpecificationExecutor<Measure> {

    List<Measure> findByPropertyOrderByMeasureTimeDesc(com.th4.edge.decoboon.property.model.Property property);

    List<Measure> findByCompany_ComIdAndProperty_PropertyIdOrderByMeasureTimeDesc(Long comId, Long propertyId);

    List<Measure> findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(
            Long comId, Collection<Long> propertyIds);

    /** 指定房屋集合内是否存在指定状态的量房事实（门禁与状态投影统一口径）。 */
    boolean existsByCompany_ComIdAndProperty_PropertyIdInAndStatus(Long comId, Collection<Long> propertyIds,
                                                                  MeasureStatus status);

    /** 指定客户集合内指定状态的量房事实（客户列表状态投影）。 */
    List<Measure> findByCompany_ComIdAndCustomer_CustIdInAndStatus(Long comId, Collection<Long> customerIds,
                                                                  MeasureStatus status);
}
