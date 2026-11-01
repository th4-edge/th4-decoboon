package com.th4.edge.decoboon.quote.service.quotes.financesummary;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteFinanceSummary;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 财务报价单行汇总。 */
public interface FinanceSummary extends Provider<Quote, QuoteFinanceSummary>, Security {
}
