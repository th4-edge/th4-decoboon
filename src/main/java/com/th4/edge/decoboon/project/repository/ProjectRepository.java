package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 项目仓储。
 */
public interface ProjectRepository extends JpaRepository<Project, Long>,
        JpaSpecificationExecutor<Project> {

    boolean existsByQuote(Quote quote);

    boolean existsByCompany_ComIdAndProperty_PropertyId(Long comId, Long propertyId);
    boolean existsByCompany_ComIdAndProperty_PropertyIdAndManager_EmpId(Long comId, Long propertyId, Long empId);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long custId);

    List<Project> findByCompany_ComIdAndCustomer_CustIdIn(Long comId, Collection<Long> custIds);

    @EntityGraph(attributePaths = {"customer", "property", "store", "manager", "designer", "support", "team", "quote"})
    List<Project> findByCompany_ComIdOrderByProjectIdDesc(Long comId);

    long countByCompany_ComId(Long comId);

    List<Project> findByCompany_ComIdAndDesigner_EmpIdOrderByProjectIdDesc(Long comId, Long empId);
    List<Project> findByCompany_ComIdAndOwner_EmpIdOrderByProjectIdDesc(Long comId, Long empId);

    List<Project> findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(Long comId, Long empId);

    List<Project> findByCompany_ComIdAndSupport_EmpIdOrderByProjectIdDesc(Long comId, Long empId);

    List<Project> findByCompany_ComIdAndQuote_CreatedBy_EmpIdOrderByProjectIdDesc(Long comId, Long empId);

        @EntityGraph(attributePaths = {"contract"})
List<Project> findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(Long comId, Long storeId);

    List<Project> findByCompany_ComIdAndCustomer_Team_TeamIdOrderByProjectIdDesc(Long comId, Long teamId);

    Optional<Project> findByProjectIdAndCompany_ComId(Long projectId, Long comId);

    Optional<Project> findFirstByCompany_ComIdAndQuote_QuoteIdOrderByProjectIdDesc(Long comId, Long quoteId);
}
