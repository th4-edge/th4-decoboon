package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerTagLink;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 客户标签关联仓储。 */
public interface CustomerTagLinkRepository extends JpaRepository<CustomerTagLink, Long> {

    List<CustomerTagLink> findByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    List<CustomerTagLink> findByCompany_ComIdAndCustomer_CustIdIn(Long comId, Collection<Long> customerIds);

    Optional<CustomerTagLink> findByCompany_ComIdAndCustomer_CustIdAndTag_TagId(Long comId, Long customerId, Long tagId);

    long deleteByCompany_ComIdAndCustomer_CustIdAndTag_TagId(Long comId, Long customerId, Long tagId);

    long deleteByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);
}
