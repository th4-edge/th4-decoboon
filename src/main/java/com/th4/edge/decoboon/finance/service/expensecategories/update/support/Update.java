package com.th4.edge.decoboon.finance.service.expensecategories.update.support;

import com.th4.edge.decoboon.finance.context.ExpenseAccessContext;
import com.th4.edge.decoboon.finance.context.ExpenseCategoryContext;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 维护企业费用分类。 */
@Note("维护费用分类")
@Properties(allow = @AllowProperties(model = ExpenseCategory.class,
        properties = {"categoryId", "code", "name", "description", "builtIn", "active", "sort"}))
@Service("expense/categories/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.expensecategories.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long categoryId;
    @AutoInject
    @Length(max = 64)
    private String name;
    @AutoInject
    @Length(max = 500)
    private String description;
    @AutoInject
    private Boolean active;
    @AutoInject
    private Integer sort;

    private final ExpenseAccessContext accessContext;
    private final ExpenseCategoryContext categoryContext;

    public Update(ExpenseAccessContext accessContext, ExpenseCategoryContext categoryContext) {
        this.accessContext = accessContext;
        this.categoryContext = categoryContext;
    }

    @Override
    @Transactional
    public ExpenseCategory call(ExpenseCategory request) {
        Employee current = accessContext.requireCategoryEditor(employee);
        return categoryContext.update(current, categoryId, name, description, active, sort);
    }
}
