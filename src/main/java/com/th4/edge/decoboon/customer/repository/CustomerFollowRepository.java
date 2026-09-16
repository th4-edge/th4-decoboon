package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 客户跟进仓储。 */
public interface CustomerFollowRepository extends JpaRepository<CustomerFollow, Long> {

    Page<CustomerFollow> findByCustomer_CustIdOrderByCreateTime_DateTimeDesc(Long customerId, Pageable pageable);
}
