package com.th4.edge.decoboon.document.service.quote.build;

import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Build extends Provider<Quote, DocumentRecord>, Security {
}
