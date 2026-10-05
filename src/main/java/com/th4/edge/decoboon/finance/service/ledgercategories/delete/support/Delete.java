package com.th4.edge.decoboon.finance.service.ledgercategories.delete.support;

import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.decoboon.finance.repository.LedgerCategoryRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 企业自定义分类逻辑删除实现。 */
@Note("删除企业自定义记账分类")
@Properties(allow = @AllowProperties(model = LedgerCategory.class,
        properties = {"categoryId", "name", "direction", "builtIn", "sort", "active"}))
@Service("finance/ledger-category/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.finance.service.ledgercategories.delete.Delete {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long categoryId;

    private final LedgerCategoryRepository repository;
    private final FinanceAccessContext accessContext;

    public Delete(LedgerCategoryRepository repository, FinanceAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public LedgerCategory call(LedgerCategory request) {
        Employee current = accessContext.requireEditor(employee);
        LedgerCategory category = repository.findByCompany_ComIdAndCategoryId(
                        current.getCompany().getComId(), categoryId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "分类不存在或无权访问")));
        // 内置一级分组不允许删除；内置二级分类允许按企业逻辑删除（停用）。
        if (Boolean.TRUE.equals(category.getBuiltIn()) && category.getParent() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "内置一级分组不可删除"));
        }
        category.setActive(false);
        if (category.getParent() == null) {
            List<LedgerCategory> children = repository.findByCompany_ComIdAndParent_CategoryId(
                    current.getCompany().getComId(), category.getCategoryId());
            children.forEach(item -> item.setActive(false));
            repository.saveAll(children);
        }
        return repository.save(category);
    }
}
