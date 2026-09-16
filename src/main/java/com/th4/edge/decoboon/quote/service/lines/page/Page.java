package com.th4.edge.decoboon.quote.service.lines.page;

import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 报价明细分页服务。 */
public interface Page extends ModelService<QuoteLine, org.springframework.data.domain.Page<QuoteLine>>, Security {
}
