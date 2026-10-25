package com.th4.edge.decoboon.finance.service.expensecategories.save;

import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 新增企业费用分类。 */
public interface Save extends ModelService<ExpenseCategory, ExpenseCategory>, Security { }
