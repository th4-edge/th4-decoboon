package com.th4.edge.decoboon.quote.service.lines.delete;

import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 报价草稿明细删除。 */
public interface Delete extends Provider<QuoteLine, Boolean>, Security {
}
