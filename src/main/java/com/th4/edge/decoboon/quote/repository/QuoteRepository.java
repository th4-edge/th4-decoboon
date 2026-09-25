package com.th4.edge.decoboon.quote.repository;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 客户报价仓储。 */
public interface QuoteRepository extends JpaRepository<Quote, Long>,
        JpaSpecificationExecutor<Quote> {

    Optional<Quote> findByQuoteIdAndCompany_ComId(Long quoteId, Long comId);

    List<Quote> findByCompany_ComIdOrderByQuoteIdDesc(Long comId);

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

    List<Quote> findByCustomer_CustIdOrderByQuoteIdDesc(Long custId);

    List<Quote> findByCustomer_CustIdIn(List<Long> customerIds);

    boolean existsByProperty_PropertyIdAndStatusIn(Long propertyId, Collection<QuoteStatus> statuses);

    /** 主键 + 企业是否存在（仅用于无能力角色 403/404 语义判定，不返回对象）。 */
    boolean existsByQuoteIdAndCompany_ComId(Long quoteId, Long comId);

    boolean existsByCustomer_CustIdAndStatusIn(Long custId, Collection<QuoteStatus> statuses);

    boolean existsByProperty_PropertyIdAndStatusInAndQuoteIdNot(Long propertyId, Collection<QuoteStatus> statuses, Long quoteId);

    boolean existsByCustomer_CustIdAndStatusInAndQuoteIdNot(Long custId, Collection<QuoteStatus> statuses, Long quoteId);

    List<Quote> findByDesign_DesignIdOrderByVersionDesc(Long designId);

    long countByDesign_DesignId(Long designId);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    long countByCompany_ComIdAndCustomer_CustId(Long comId, Long customerId);

    long countByCompany_ComIdAndProperty_PropertyId(Long comId, Long propertyId);

    long countByCompany_ComIdAndCustomer_CustIdAndPackageTemplate_TemplateId(
            Long comId, Long customerId, Long templateId);

    long countByCompany_ComIdAndProperty_PropertyIdAndPackageTemplate_TemplateId(
            Long comId, Long propertyId, Long templateId);
}
