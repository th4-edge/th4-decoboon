package com.th4.edge.decoboon.quote.service.workbook.importer;

import com.th4.edge.decoboon.quote.model.QuoteWorkbookImportRequest;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Import extends Provider<QuoteWorkbookImportRequest, ImportResult>, Security {
}
