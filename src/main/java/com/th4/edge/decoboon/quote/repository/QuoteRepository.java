package com.th4.edge.decoboon.quote.repository;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 客户报价仓储。 */
public interface QuoteRepository extends JpaRepository<Quote, Long>,
        JpaSpecificationExecutor<Quote> {

    /** 动态筛选分页；预取页面序列化需要的一层关联，避免事务边界外触发懒加载。 */
    @Override
    @EntityGraph(attributePaths = {"customer", "property", "design", "createdBy"})
    Page<Quote> findAll(Specification<Quote> specification, Pageable pageable);

    /**
     * 设计师待谈单工作队列的 ID 分页。状态优先级、停留时间排序、关键词、角色范围和
     * Dormant/Lost 与单房流失排除均在 SQL 完成，避免先加载企业全部报价再在 Java 中过滤切页。
     */
    @Query(value = """
            select q.quote_id
            from quote q
            join cust_customer c on c.cust_id = q.customer_id
            join db_property p on p.property_id = q.property_id
            where q.com_id = :comId
              and q.status in ('Draft', 'Submitted', 'Rejected')
              and c.status in ('New', 'Following', 'PendingMeasure', 'Quoted')
              and not exists (
                  select 1 from cust_customer_loss cl
                  where cl.com_id = q.com_id
                    and cl.property_id = q.property_id
                    and cl.restored_at is null
              )
              and (:allCompany = true or q.store_id = :storeId)
              and (:designerId is null or q.emp_id = :designerId)
              and (:statusFilter is null or q.status = :statusFilter)
              and (:focusQuoteId is null or q.quote_id = :focusQuoteId)
              and (:keyword = '' or lower(q.no) like :pattern
                   or lower(c.name) like :pattern
                   or lower(c.tel_no) like :pattern
                   or lower(coalesce(p.community, c.community, '')) like :pattern)
            order by case q.status when 'Rejected' then 0 when 'Draft' then 1 else 2 end,
                     case q.status
                       when 'Rejected' then timestampdiff(second,
                         coalesce(q.rejected_at, from_unixtime(q.create_date_time / 1000)), now())
                       when 'Draft' then timestampdiff(second, from_unixtime(q.create_date_time / 1000), now())
                       else timestampdiff(second,
                         coalesce(q.submitted_at, from_unixtime(q.create_date_time / 1000)), now())
                     end asc,
                     q.quote_id desc
            """, countQuery = """
            select count(q.quote_id)
            from quote q
            join cust_customer c on c.cust_id = q.customer_id
            join db_property p on p.property_id = q.property_id
            where q.com_id = :comId
              and q.status in ('Draft', 'Submitted', 'Rejected')
              and c.status in ('New', 'Following', 'PendingMeasure', 'Quoted')
              and not exists (
                  select 1 from cust_customer_loss cl
                  where cl.com_id = q.com_id
                    and cl.property_id = q.property_id
                    and cl.restored_at is null
              )
              and (:allCompany = true or q.store_id = :storeId)
              and (:designerId is null or q.emp_id = :designerId)
              and (:statusFilter is null or q.status = :statusFilter)
              and (:focusQuoteId is null or q.quote_id = :focusQuoteId)
              and (:keyword = '' or lower(q.no) like :pattern
                   or lower(c.name) like :pattern
                   or lower(c.tel_no) like :pattern
                   or lower(coalesce(p.community, c.community, '')) like :pattern)
            """, nativeQuery = true)
    Page<Long> findWorkbenchQuoteIds(@Param("comId") Long comId,
                                     @Param("allCompany") boolean allCompany,
                                     @Param("storeId") Long storeId,
                                     @Param("designerId") Long designerId,
                                     @Param("statusFilter") String statusFilter,
                                     @Param("focusQuoteId") Long focusQuoteId,
                                     @Param("keyword") String keyword,
                                     @Param("pattern") String pattern,
                                     Pageable pageable);

    /** 只批量预取工作队列页面需要的客户与房屋摘要，顺序由调用方按 ID 页恢复。 */
    @EntityGraph(attributePaths = {"customer", "property"})
    @Query("select q from Quote q where q.quoteId in :quoteIds")
    List<Quote> findWorkbenchQuotes(@Param("quoteIds") Collection<Long> quoteIds);

    /** 签约工作队列：SQL 先过滤合同/项目状态并分页，页面只批量读取当前页报价。 */
    @Query(value = """
            select q.quote_id
            from quote q
            join cust_customer c on c.cust_id = q.customer_id
            join db_property p on p.property_id = q.property_id
            left join db_project_contract ct on ct.quote_id = q.quote_id and ct.com_id = q.com_id
            left join db_project pj on pj.project_id = ct.project_id
            where q.com_id = :comId
              and q.status in ('Accepted', 'Converted')
              and c.status not in ('Dormant', 'Lost')
              and not exists (
                  select 1 from cust_customer_loss cl
                  where cl.com_id = q.com_id
                    and cl.property_id = q.property_id
                    and cl.restored_at is null
              )
              and (
                  :allCompany = true
                  or (:storeId is not null and q.store_id = :storeId)
                  or (:teamId is not null and c.team_id = :teamId)
                  or (:creatorId is not null and q.emp_id = :creatorId)
              )
              and (:focusQuoteId is null or q.quote_id = :focusQuoteId)
              and (:keyword = '' or lower(q.no) like :pattern
                   or lower(coalesce(ct.no, '')) like :pattern
                   or lower(c.name) like :pattern
                   or lower(c.tel_no) like :pattern
                   or lower(coalesce(p.community, c.community, '')) like :pattern
                   or lower(coalesce(p.address, c.addr, '')) like :pattern)
              and (
                  :filterValue is null or :filterValue = '' or :filterValue = 'all'
                  or (:filterValue = 'toCreate' and ct.contract_id is null)
                  or (:filterValue = 'waitingConfirm' and ct.status in ('PendingSign', 'Signed'))
                  or (:filterValue = 'effective' and ct.status in
                      ('Effective', 'Active', 'Changing', 'Changed', 'Completed', 'Settled', 'Closed', 'Terminated'))
                  or (:filterValue = 'pending' and ct.contract_id is null)
                  or (:filterValue = 'waiting' and ct.status in ('PendingSign', 'Signed'))
                  or (:filterValue = 'fulfilling' and pj.status in ('Preparing', 'Ongoing', 'Suspended', 'Accepting'))
                  or (:filterValue = 'done' and pj.status in ('Finished', 'Settled'))
              )
            order by
              case when q.status = 'Converted' or ct.status in
                  ('Signed', 'Effective', 'Active', 'Changing', 'Changed', 'Completed', 'Settled', 'Closed', 'Terminated')
                  or ct.project_id is not null then 1 else 0 end asc,
              coalesce(q.accepted_at, from_unixtime(q.create_date_time / 1000)) asc,
              q.quote_id desc
            """, countQuery = """
            select count(q.quote_id)
            from quote q
            join cust_customer c on c.cust_id = q.customer_id
            join db_property p on p.property_id = q.property_id
            left join db_project_contract ct on ct.quote_id = q.quote_id and ct.com_id = q.com_id
            left join db_project pj on pj.project_id = ct.project_id
            where q.com_id = :comId
              and q.status in ('Accepted', 'Converted')
              and c.status not in ('Dormant', 'Lost')
              and not exists (
                  select 1 from cust_customer_loss cl
                  where cl.com_id = q.com_id
                    and cl.property_id = q.property_id
                    and cl.restored_at is null
              )
              and (
                  :allCompany = true
                  or (:storeId is not null and q.store_id = :storeId)
                  or (:teamId is not null and c.team_id = :teamId)
                  or (:creatorId is not null and q.emp_id = :creatorId)
              )
              and (:focusQuoteId is null or q.quote_id = :focusQuoteId)
              and (:keyword = '' or lower(q.no) like :pattern
                   or lower(coalesce(ct.no, '')) like :pattern
                   or lower(c.name) like :pattern
                   or lower(c.tel_no) like :pattern
                   or lower(coalesce(p.community, c.community, '')) like :pattern
                   or lower(coalesce(p.address, c.addr, '')) like :pattern)
              and (
                  :filterValue is null or :filterValue = '' or :filterValue = 'all'
                  or (:filterValue = 'toCreate' and ct.contract_id is null)
                  or (:filterValue = 'waitingConfirm' and ct.status in ('PendingSign', 'Signed'))
                  or (:filterValue = 'effective' and ct.status in
                      ('Effective', 'Active', 'Changing', 'Changed', 'Completed', 'Settled', 'Closed', 'Terminated'))
                  or (:filterValue = 'pending' and ct.contract_id is null)
                  or (:filterValue = 'waiting' and ct.status in ('PendingSign', 'Signed'))
                  or (:filterValue = 'fulfilling' and pj.status in ('Preparing', 'Ongoing', 'Suspended', 'Accepting'))
                  or (:filterValue = 'done' and pj.status in ('Finished', 'Settled'))
              )
            """, nativeQuery = true)
    Page<Long> findWorkbenchSignQuoteIds(@Param("comId") Long comId,
                                         @Param("allCompany") boolean allCompany,
                                         @Param("storeId") Long storeId,
                                         @Param("teamId") Long teamId,
                                         @Param("creatorId") Long creatorId,
                                         @Param("filterValue") String filterValue,
                                         @Param("focusQuoteId") Long focusQuoteId,
                                         @Param("keyword") String keyword,
                                         @Param("pattern") String pattern,
                                         Pageable pageable);

    /** 批量预取签约当前页报价及设计责任来源。 */
    @EntityGraph(attributePaths = {"customer", "property", "design"})
    @Query("select q from Quote q where q.quoteId in :quoteIds")
    List<Quote> findSignWorkbenchQuotes(@Param("quoteIds") Collection<Long> quoteIds);

    Optional<Quote> findByQuoteIdAndCompany_ComId(Long quoteId, Long comId);

    List<Quote> findByCompany_ComIdOrderByQuoteIdDesc(Long comId);

    /** 按客户集合取报价（用于客户经营投影的最新报价，量级 = 当前页客户数）。 */
    List<Quote> findByCompany_ComIdAndCustomer_CustIdInOrderByQuoteIdDesc(Long comId,
                                                                        java.util.Collection<Long> custIds);

    long countByCompany_ComId(Long comId);

    List<Quote> findTop200ByCompany_ComIdOrderByQuoteIdDesc(Long comId);

    List<Quote> findByCompany_ComIdAndCreatedBy_EmpIdOrderByQuoteIdDesc(Long comId, Long empId);

    List<Quote> findByCompany_ComIdAndCreatedBy_EmpIdAndStatusInOrderByQuoteIdDesc(
            Long comId, Long empId, Collection<QuoteStatus> statuses);

    List<Quote> findByCompany_ComIdAndStatusInOrderByQuoteIdDesc(
            Long comId, Collection<QuoteStatus> statuses);

    List<Quote> findByCompany_ComIdAndCustomer_Store_StoreIdAndStatusInOrderByQuoteIdDesc(
            Long comId, Long storeId, Collection<QuoteStatus> statuses);

    List<Quote> findByCompany_ComIdAndCustomer_Team_TeamIdAndStatusInOrderByQuoteIdDesc(
            Long comId, Long teamId, Collection<QuoteStatus> statuses);

    List<Quote> findByCompany_ComIdAndProperty_PropertyIdInAndStatusIn(
            Long comId, Collection<Long> propertyIds, Collection<QuoteStatus> statuses);

    @org.springframework.data.jpa.repository.Query("""
            select distinct q from Quote q
            where q.company.comId = :comId
              and (q.customer.owner.empId = :empId or q.property.salesman.empId = :empId)
            order by q.quoteId desc
            """)
    List<Quote> findMarketerVisible(@org.springframework.data.repository.query.Param("comId") Long comId,
                                    @org.springframework.data.repository.query.Param("empId") Long empId);

    @org.springframework.data.jpa.repository.Query("""
            select distinct q from Quote q
            where q.quoteId = :quoteId and q.company.comId = :comId
              and (q.customer.owner.empId = :empId or q.property.salesman.empId = :empId)
            """)
    Optional<Quote> findMarketerVisibleById(@org.springframework.data.repository.query.Param("quoteId") Long quoteId,
                                           @org.springframework.data.repository.query.Param("comId") Long comId,
                                           @org.springframework.data.repository.query.Param("empId") Long empId);

    List<Quote> findByCompany_ComIdAndCustomer_Store_StoreIdOrderByQuoteIdDesc(Long comId, Long storeId);

    List<Quote> findByCompany_ComIdAndCustomer_Team_TeamIdOrderByQuoteIdDesc(Long comId, Long teamId);

    List<Quote> findByProperty_PropertyIdOrderByQuoteIdDesc(Long propertyId);

    List<Quote> findByCompany_ComIdAndProperty_PropertyIdOrderByQuoteIdDesc(Long comId, Long propertyId);

    List<Quote> findByCustomer_CustIdOrderByQuoteIdDesc(Long custId);

    List<Quote> findByCustomer_CustIdIn(List<Long> customerIds);

    boolean existsByProperty_PropertyIdAndStatusIn(Long propertyId, Collection<QuoteStatus> statuses);

    /** 主键 + 企业是否存在（仅用于无能力角色 403/404 语义判定，不返回对象）。 */
    boolean existsByQuoteIdAndCompany_ComId(Long quoteId, Long comId);

    boolean existsByCustomer_CustIdAndStatusIn(Long custId, Collection<QuoteStatus> statuses);

    boolean existsByProperty_PropertyIdAndStatusInAndQuoteIdNot(Long propertyId, Collection<QuoteStatus> statuses, Long quoteId);

    boolean existsByCustomer_CustIdAndStatusInAndQuoteIdNot(Long custId, Collection<QuoteStatus> statuses, Long quoteId);

    List<Quote> findByDesign_DesignIdOrderByVersionDesc(Long designId);

    @EntityGraph(attributePaths = {"design"})
    List<Quote> findByDesign_DesignIdInOrderByVersionDesc(Collection<Long> designIds);

    long countByDesign_DesignId(Long designId);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    long countByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    long countByCompany_ComIdAndProperty_PropertyId(Long comId, Long propertyId);

    long countByCompany_ComIdAndCustomer_CustIdAndPackageTemplate_TemplateId(
            Long comId, Long customerId, Long templateId);

    long countByCompany_ComIdAndProperty_PropertyIdAndPackageTemplate_TemplateId(
            Long comId, Long propertyId, Long templateId);


    /**
     * 正常经营来源、可构成报价机会的报价（排除草稿与已被替代）：门店按**报价自身门店**收窄，
     * 来源与状态条件在 SQL 完成；机会分组口径仍由 `QuoteAcceptanceContext` 统一计算。
     */
    @EntityGraph(attributePaths = {"customer", "property", "createdBy", "store"})
    @Query("select q from Quote q left join q.store qs where q.company.comId = :comId "
            + "and (:allStores = true or qs.storeId = :storeId) "
            + "and (q.businessOrigin is null or q.businessOrigin = :normal) "
            + "and (q.status is null or q.status not in (:draft, :superseded)) order by q.quoteId desc")
    List<Quote> findOpportunityQuotesByStoreScope(@Param("comId") Long comId,
                                                  @Param("normal") com.th4.edge.decoboon.common.configure.BusinessOrigin normal,
                                                  @Param("draft") com.th4.edge.decoboon.quote.configure.QuoteStatus draft,
                                                  @Param("superseded") com.th4.edge.decoboon.quote.configure.QuoteStatus superseded,
                                                  @Param("allStores") boolean allStores,
                                                  @Param("storeId") Long storeId);
}
