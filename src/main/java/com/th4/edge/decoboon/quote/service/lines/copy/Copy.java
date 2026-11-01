package com.th4.edge.decoboon.quote.service.lines.copy;

import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 将草稿报价中的一条分项复制为新的增项行。 */
public interface Copy extends Provider<QuoteLine, QuoteLine>, Security {
}
