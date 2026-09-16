package com.th4.edge.decoboon.finance.service.receivables.page;

import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 应收计划分页。 */
public interface Page extends ModelService<Receivable, org.springframework.data.domain.Page<Receivable>>, Security {
}
