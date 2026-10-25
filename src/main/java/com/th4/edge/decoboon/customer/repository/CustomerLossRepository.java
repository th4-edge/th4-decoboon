package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.configure.CustomerLossScope;
import com.th4.edge.decoboon.customer.model.CustomerLoss;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 客户流失记录仓储。 */
public interface CustomerLossRepository extends JpaRepository<CustomerLoss, Long> {

    @EntityGraph(attributePaths = {"customer", "property", "lostBy", "restoredBy"})
    Optional<CustomerLoss> findByCompany_ComIdAndRequestId(Long comId, String requestId);

    @EntityGraph(attributePaths = {"customer", "property", "lostBy", "restoredBy"})
    Optional<CustomerLoss> findFirstByCompany_ComIdAndCustomer_CustIdAndScopeAndRestoredAtIsNullOrderByLossIdDesc(
            Long comId, Long customerId, CustomerLossScope scope);

    @EntityGraph(attributePaths = {"customer", "property", "lostBy", "restoredBy"})
    Optional<CustomerLoss> findFirstByCompany_ComIdAndProperty_PropertyIdAndRestoredAtIsNullOrderByLossIdDesc(
            Long comId, Long propertyId);

    @EntityGraph(attributePaths = {"customer", "property", "lostBy", "restoredBy"})
    Optional<CustomerLoss> findByCompany_ComIdAndLossId(Long comId, Long lossId);

    @EntityGraph(attributePaths = {"customer", "property", "lostBy", "restoredBy"})
    @Query("""
            select l from CustomerLoss l
            where l.company.comId = :comId
              and l.customer.custId in :customerIds
              and l.restoredAt is null
            order by l.lossId desc
            """)
    List<CustomerLoss> findActiveByCustomerIds(@Param("comId") Long comId,
                                               @Param("customerIds") Collection<Long> customerIds);

    @Query("""
            select l.property.propertyId from CustomerLoss l
            where l.company.comId = :comId
              and l.customer.custId = :customerId
              and l.scope = com.th4.edge.decoboon.customer.configure.CustomerLossScope.Property
              and l.restoredAt is null
            """)
    List<Long> findActivePropertyIds(@Param("comId") Long comId,
                                     @Param("customerId") Long customerId);

    long countByCompany_ComIdAndCustomer_CustIdAndRestoredAtIsNull(Long comId, Long customerId);

    @EntityGraph(attributePaths = {"customer", "property", "lostBy", "restoredBy"})
    Page<CustomerLoss> findByCompany_ComIdAndCustomer_CustIdOrderByLossIdDesc(
            Long comId, Long customerId, Pageable pageable);
}
