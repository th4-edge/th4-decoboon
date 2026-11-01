package com.th4.edge.decoboon.design.repository;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 设计方案仓储。 */
public interface DesignRepository extends JpaRepository<Design, Long>,
        JpaSpecificationExecutor<Design> {

    @Override
    @EntityGraph(attributePaths = {"customer", "property", "owner", "store"})
    Page<Design> findAll(Specification<Design> specification, Pageable pageable);


    List<Design> findByCompany_ComIdOrderByDesignIdDesc(Long comId);
    java.util.Optional<Design> findFirstByCompany_ComIdAndProperty_PropertyIdAndDesignTemplateVersion_VersionId(
            Long comId, Long propertyId, Long versionId);

    List<Design> findByCompany_ComIdAndCustomer_CustIdOrderByVersionDesc(Long comId, Long customerId);

    List<Design> findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(Long comId, Collection<Long> propertyIds);

    List<Design> findByCompany_ComIdAndCustomer_CustIdAndStatusInOrderByVersionDesc(
            Long comId, Long customerId, Collection<com.th4.edge.decoboon.design.configure.DesignStatus> statuses);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    long countByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    @Query("select d.designTemplateVersion.template.templateId, count(d) from Design d "
            + "where d.company.comId = :comId and d.designTemplateVersion.template.templateId in :templateIds "
            + "group by d.designTemplateVersion.template.templateId")
    List<Object[]> countUsageByTemplateIds(@Param("comId") Long comId, @Param("templateIds") Collection<Long> templateIds);
}
