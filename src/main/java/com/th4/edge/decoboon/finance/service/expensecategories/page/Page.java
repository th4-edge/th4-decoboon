package com.th4.edge.decoboon.finance.service.expensecategories.page;

import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 费用分类选择器分页。 */
public interface Page extends Provider<ExpenseCategory, org.springframework.data.domain.Page<ExpenseCategory>>, Security {
}
