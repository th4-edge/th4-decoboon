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

    List<Quote> findByCompany_ComIdAndCreatedBy_EmpIdOrderByQuoteIdDesc(Long comId, Long empId);

    List<Quote> findByCompany_ComIdAndCustomer_Store_StoreIdOrderByQuoteIdDesc(Long comId, Long storeId);

    List<Quote> findByCompany_ComIdAndCustomer_Team_TeamIdOrderByQuoteIdDesc(Long comId, Long teamId);

    List<Quote> findByProperty_PropertyIdOrderByQuoteIdDesc(Long propertyId);

    List<Quote> findByCustomer_CustIdOrderByQuoteIdDesc(Long custId);

    List<Quote> findByCustomer_CustIdIn(List<Long> customerIds);

    boolean existsByProperty_PropertyIdAndStatusIn(Long propertyId, Collection<QuoteStatus> statuses);

    boolean existsByCustomer_CustIdAndStatusIn(Long custId, Collection<QuoteStatus> statuses);

    boolean existsByProperty_PropertyIdAndStatusInAndQuoteIdNot(Long propertyId, Collection<QuoteStatus> statuses, Long quoteId);

    boolean existsByCustomer_CustIdAndStatusInAndQuoteIdNot(Long custId, Collection<QuoteStatus> statuses, Long quoteId);

    List<Quote> findByDesign_DesignIdOrderByVersionDesc(Long designId);

    long countByDesign_DesignId(Long designId);
}
