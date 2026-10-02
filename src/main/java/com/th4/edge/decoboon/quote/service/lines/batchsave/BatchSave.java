package com.th4.edge.decoboon.quote.service.lines.batchsave;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface BatchSave extends Provider<Quote, Quote>, Security {
}
