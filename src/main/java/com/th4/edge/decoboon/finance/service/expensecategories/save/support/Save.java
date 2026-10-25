package com.th4.edge.decoboon.finance.service.expensecategories.save.support;

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
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 新增企业费用分类。 */
@Note("新增费用分类")
@Properties(allow = @AllowProperties(model = ExpenseCategory.class,
        properties = {"categoryId", "code", "name", "description", "builtIn", "active", "sort"}))
@Service("expense/categories/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.expensecategories.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotEmpty
    @Length(max = 64)
    private String name;
    @AutoInject
    @Length(max = 500)
    private String description;
    @AutoInject
    private Integer sort;

    private final ExpenseAccessContext accessContext;
    private final ExpenseCategoryContext categoryContext;

    public Save(ExpenseAccessContext accessContext, ExpenseCategoryContext categoryContext) {
        this.accessContext = accessContext;
        this.categoryContext = categoryContext;
    }

    @Override
    @Transactional
    public ExpenseCategory call(ExpenseCategory request) {
        Employee current = accessContext.requireCategoryEditor(employee);
        return categoryContext.saveCustom(current, name, description, sort);
    }
}
