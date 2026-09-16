package com.th4.edge.decoboon.project.service.manager.quotes.page;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目经理负责项目来源报价分页。 */
public interface Page extends Provider<Quote, org.springframework.data.domain.Page<Quote>>, Security {
}
