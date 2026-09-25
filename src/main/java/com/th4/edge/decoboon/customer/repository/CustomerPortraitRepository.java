package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 客户画像仓储。 */
public interface CustomerPortraitRepository extends JpaRepository<CustomerPortrait, Long> {

    Optional<CustomerPortrait> findByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    List<CustomerPortrait> findByCompany_ComIdAndCustomer_CustIdIn(Long comId, Collection<Long> customerIds);

    long deleteByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);
}
