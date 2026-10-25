package com.th4.edge.decoboon.quote.service.quotes.financereport;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteDesignerAggregate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 财务报价按设计师聚合（DEC-036 正模式：聚合 SQL 直接算，前端不循环累加）。 */
public interface FinanceReport extends Provider<Quote, List<QuoteDesignerAggregate>>, Security {
}
