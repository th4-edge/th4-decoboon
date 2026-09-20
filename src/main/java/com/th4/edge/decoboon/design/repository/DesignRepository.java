package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 设计方案仓储。 */
public interface DesignRepository extends JpaRepository<Design, Long>,
        JpaSpecificationExecutor<Design> {

    List<Design> findByCompany_ComIdAndCustomer_CustIdOrderByVersionDesc(Long comId, Long customerId);

    List<Design> findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(Long comId, Collection<Long> propertyIds);

    List<Design> findByCompany_ComIdAndCustomer_CustIdAndStatusInOrderByVersionDesc(
            Long comId, Long customerId, Collection<com.th4.edge.decoboon.design.configure.DesignStatus> statuses);

    long countByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);
}
