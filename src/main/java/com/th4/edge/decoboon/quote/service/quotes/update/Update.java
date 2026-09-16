package com.th4.edge.decoboon.quote.service.quotes.update;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/**
 * 客户报价状态推进服务。
 */
public interface Update extends Provider<Quote, Quote>, Security {
}
