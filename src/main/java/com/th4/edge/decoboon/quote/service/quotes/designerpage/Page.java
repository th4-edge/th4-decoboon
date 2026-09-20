package com.th4.edge.decoboon.quote.service.quotes.designerpage;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.service.ModelService;
import com.th4.edge.framework.provider.security.Security;

/** 设计师报价分页，响应不包含成本字段。 */
public interface Page extends ModelService<Quote, org.springframework.data.domain.Page<Quote>>, Security {
}
