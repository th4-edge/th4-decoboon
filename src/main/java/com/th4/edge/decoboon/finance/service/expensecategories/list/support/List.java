package com.th4.edge.decoboon.finance.service.expensecategories.list.support;

import com.th4.edge.decoboon.finance.context.ExpenseAccessContext;
import com.th4.edge.decoboon.finance.context.ExpenseCategoryContext;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 当前企业费用分类列表。 */
@Note("费用分类列表")
@Properties(allow = @AllowProperties(model = ExpenseCategory.class,
        properties = {"categoryId", "code", "name", "description", "builtIn", "active", "sort"}))
@Service("expense/categories/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.finance.service.expensecategories.list.List {

    @User
    private Employee employee;
    private final ExpenseAccessContext accessContext;
    private final ExpenseCategoryContext categoryContext;

    public List(ExpenseAccessContext accessContext, ExpenseCategoryContext categoryContext) {
        this.accessContext = accessContext;
        this.categoryContext = categoryContext;
    }

    @Override
    @Transactional
    public java.util.List<ExpenseCategory> call(ExpenseCategory request) {
        Employee current = accessContext.requireSubmitter(employee);
        return categoryContext.list(current);
    }
}
