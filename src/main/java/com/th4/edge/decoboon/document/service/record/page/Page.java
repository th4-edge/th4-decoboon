package com.th4.edge.decoboon.document.service.record.page;

import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Page extends Provider<Quote, java.util.List<DocumentRecord>>, Security {
}
