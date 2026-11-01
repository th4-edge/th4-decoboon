package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 项目仓储。
 */
public interface ProjectRepository extends JpaRepository<Project, Long>,
        JpaSpecificationExecutor<Project> {

    @Override
    @EntityGraph(attributePaths = {"company", "store", "team", "owner", "customer", "property", "quote", "contract", "manager", "designer", "support"})
    Page<Project> findAll(Specification<Project> specification, Pageable pageable);


    boolean existsByQuote(Quote quote);

    /**
     * 按项目编号查询：业主项目码的 scene 使用项目编号（对外已可读、长度 ≤32、不含敏感信息）。
     *
     * <p>带 {@link EntityGraph} 预取业主指纹所需关联，避免扫码进入在事务外读懒加载。</p>
     */
    @EntityGraph(attributePaths = {"customer", "property", "property.customer", "company"})
    Optional<Project> findByNo(String no);

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

    /** 财务项目访问判定：预取客户门店快照，避免 open-in-view=false 时读取门店归属越界。 */
    @Query("""
            select p from Project p
            join fetch p.customer c
            left join fetch c.store
            where p.projectId = :projectId and p.company.comId = :comId
            """)
    Optional<Project> findScopeDetailByProjectIdAndCompany_ComId(@Param("projectId") Long projectId,
                                                                 @Param("comId") Long comId);

    /**
     * 项目详情预取：仅预取详情响应直接需要的四个聚合根关联。
     * 其余二级关联由 Provider 在只读事务内显式初始化，避免 OSIV=false 时序列化越界。
     */
    @EntityGraph(attributePaths = {"customer", "property", "quote", "designer"})
    Optional<Project> findDetailedByProjectIdAndCompany_ComId(Long projectId, Long comId);
    @EntityGraph(attributePaths = {"customer", "property", "manager", "designer", "quote"})
    Optional<Project> findFinanceDetailByProjectIdAndCompany_ComId(Long projectId, Long comId);

    /** 项目账本读取：按统一 Specification 取项目，再由账本上下文批量回填财务事实。 */
    default List<Project> findLedgerRows(org.springframework.data.jpa.domain.Specification<Project> specification) {
        return findAll(specification);
    }

    /** 项目账本分页后的批量详情读取：一次预取页面与成本计算所需关联。 */
    @Query("""
            select distinct p from Project p
            left join fetch p.customer
            left join fetch p.property
            left join fetch p.manager
            left join fetch p.quote
            where p.projectId in :ids
            """)
    List<Project> findDetailsByIdIn(@Param("ids") Collection<Long> ids);

    /** 门店经营利润：按项目自身门店快照聚合（DEC-070/MET-011）。 */
    List<Project> findByCompany_ComIdAndStore_StoreId(Long comId, Long storeId);

    Optional<Project> findFirstByCompany_ComIdAndQuote_QuoteIdOrderByProjectIdDesc(Long comId, Long quoteId);


    /**
     * 门店维度项目（`DEC-067` BOSS-STORE-09）：全部门店不过滤（含 `store` 为空的未归属项目）；
     * 单店按「项目 `store` 缺失回退客户门店」收窄。SQL 内完成门店条件，不再全量加载后 Java 过滤。
     *
     * <p>`contract` 必须列入 {@code @EntityGraph}（P0B-02 续：`BR-031`/`DEC-036` 循环内零查询）：
     * {@code Project.contract} 是 `@OneToOne(mappedBy = "project", fetch = LAZY)` 的**反向**一对一，
     * Hibernate 无法为反向一对一建代理（无法在不查库的前提下判断「是否无合同」），因此**每加载一行
     * Project 都会补发一条 `select ... from db_project_contract where project_id = ?`**；
     * 实测 `statistic/performance/report` 在这个关联上随项目数线性增长（18 项目 → 18 条）。
     * 预先 JOIN FETCH 后该关联已初始化，不再逐行补查。</p>
     */
    @EntityGraph(attributePaths = {"customer", "property", "store", "manager", "contract"})
    @Query("select p from Project p left join p.store ps left join p.customer pcu left join pcu.store pcs "
            + "where p.company.comId = :comId and (:allStores = true or coalesce(ps.storeId, pcs.storeId) = :storeId) "
            + "order by p.projectId desc")
    List<Project> findByCompanyStoreScope(@Param("comId") Long comId,
                                          @Param("allStores") boolean allStores,
                                          @Param("storeId") Long storeId);
}
