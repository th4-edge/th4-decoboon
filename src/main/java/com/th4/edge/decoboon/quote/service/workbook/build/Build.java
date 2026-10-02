package com.th4.edge.decoboon.quote.service.workbook.build;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteWorkbookFile;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Build extends Provider<Quote, QuoteWorkbookFile>, Security {
}
