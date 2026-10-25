package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 项目合同基线仓储。 */
public interface ProjectContractRepository extends JpaRepository<ProjectContract, Long>,
        JpaSpecificationExecutor<ProjectContract> {

    Optional<ProjectContract> findByProject(Project project);

    /** 合同详情页所需的最小聚合根预取；其余二级关联在 Provider 只读事务内初始化。 */
    @EntityGraph(attributePaths = {"project", "customer", "property", "quote"})
    Optional<ProjectContract> findDetailedByProject(Project project);

    @EntityGraph(attributePaths = {"project", "customer", "property", "quote"})
    Optional<ProjectContract> findDetailedByQuote_QuoteId(Long quoteId);

    Optional<ProjectContract> findByQuote_QuoteId(Long quoteId);

    List<ProjectContract> findByQuote_QuoteIdIn(Collection<Long> quoteIds);

    /** 签约当前页合同与项目一次批量预取。 */
    @EntityGraph(attributePaths = {"project", "quote"})
    @Query("select c from ProjectContract c where c.quote.quoteId in :quoteIds")
    List<ProjectContract> findWorkbenchByQuoteIds(@Param("quoteIds") Collection<Long> quoteIds);

    Optional<ProjectContract> findByProject_ProjectIdAndCompany_ComId(Long projectId, Long comId);

    Optional<ProjectContract> findByContractIdAndCompany_ComId(Long contractId, Long comId);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    /** 指定客户是否存在不在终态集合内的正式合同。 */
    boolean existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
            Long comId, Long customerId, Collection<ContractStatus> statuses);

    /** 指定房屋是否存在不在终态集合内的正式合同。 */
    boolean existsByCompany_ComIdAndProperty_PropertyIdAndStatusNotIn(
            Long comId, Long propertyId, Collection<ContractStatus> statuses);

    boolean existsByProject_ProjectId(Long projectId);

    boolean existsByProperty_PropertyId(Long propertyId);

    @EntityGraph(attributePaths = {"project", "customer", "property", "store", "owner", "quote"})
    List<ProjectContract> findByCompany_ComIdOrderByContractIdDesc(Long comId);

    /** 按客户集合取合同（用于客户经营投影的最新合同，量级 = 当前页客户数）。 */
    List<ProjectContract> findByCompany_ComIdAndCustomer_CustIdInOrderByContractIdDesc(
            Long comId, java.util.Collection<Long> custIds);

    long countByCompany_ComId(Long comId);

    List<ProjectContract> findTop200ByCompany_ComIdOrderByContractIdDesc(Long comId);


    /**
     * 正常经营来源、签约日区间内的合同（驾驶舱毛利/签约额聚合用）：门店按**合同自身门店**收窄，
     * 区间与来源条件全部在 SQL 完成。
     */
    @Query("select ct from ProjectContract ct left join ct.store cs left join ct.project cp "
            + "left join cp.customer cc left join cp.property cpr "
            + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to "
            + "and (ct.businessOrigin is null or ct.businessOrigin = :normal) "
            + "and (:allStores = true or cs.storeId = :storeId) order by ct.contractId desc")
    List<ProjectContract> findNormalSignedBetweenByStoreScope(@Param("comId") Long comId,
                                                             @Param("from") java.time.LocalDate from,
                                                             @Param("to") java.time.LocalDate to,
                                                             @Param("normal") com.th4.edge.decoboon.common.configure.BusinessOrigin normal,
                                                             @Param("allStores") boolean allStores,
                                                             @Param("storeId") Long storeId);
}
