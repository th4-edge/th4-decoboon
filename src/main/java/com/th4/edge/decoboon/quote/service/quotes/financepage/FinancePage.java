package com.th4.edge.decoboon.quote.service.quotes.financepage;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 财务报价分页，只读，成本仅财务可见（BR-052）。 */
public interface FinancePage extends ModelService<Quote, org.springframework.data.domain.Page<Quote>>, Security {
}
