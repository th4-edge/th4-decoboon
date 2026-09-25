package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

/** 客户诉求仓储。 */
public interface RequirementRepository extends JpaRepository<Requirement, Long>,
        JpaSpecificationExecutor<Requirement> {

    List<Requirement> findByCompany_ComIdOrderByRequirementIdDesc(Long comId);

    List<Requirement> findByCompany_ComIdAndCustomer_CustIdOrderByRequirementIdDesc(Long comId, Long customerId);

    List<Requirement> findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(
            Long comId, Collection<Long> propertyIds);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);
}
