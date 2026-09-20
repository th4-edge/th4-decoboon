package com.th4.edge.decoboon.quote.service.lines.save;

import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 报价草稿明细调整。 */
public interface Save extends Provider<QuoteLine, QuoteLine>, Security {
}
