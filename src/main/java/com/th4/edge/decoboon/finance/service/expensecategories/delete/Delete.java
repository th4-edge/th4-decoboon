package com.th4.edge.decoboon.finance.service.expensecategories.delete;

import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 停用企业费用分类。 */
public interface Delete extends ModelService<ExpenseCategory, ExpenseCategory>, Security { }
