package com.th4.edge.decoboon.finance.service.ledgercategories.save.support;

import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.LedgerCategoryContext;
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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业自定义分类保存实现。 */
@Note("保存企业自定义记账分类")
@Properties(allow = @AllowProperties(model = LedgerCategory.class,
        properties = {"categoryId", "name", "direction", "builtIn", "sort", "active"}))
@Service("finance/ledger-category/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.ledgercategories.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotEmpty
    @Length(max = 64)
    private String name;
    @AutoInject
    @NotNull
    private LedgerDirection direction;
    @AutoInject
    private Long parentId;

    private final LedgerCategoryRepository repository;
    private final LedgerCategoryContext categoryContext;
    private final FinanceAccessContext accessContext;

    public Save(LedgerCategoryRepository repository, LedgerCategoryContext categoryContext,
                FinanceAccessContext accessContext) {
        this.repository = repository;
        this.categoryContext = categoryContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public LedgerCategory call(LedgerCategory request) {
        Employee current = accessContext.requireEditor(employee);
        LedgerCategory parent = null;
        if (parentId != null) {
            parent = repository.findByCompany_ComIdAndCategoryId(current.getCompany().getComId(), parentId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "上级分类不存在或无权访问")));
            if (!Boolean.TRUE.equals(parent.getActive()) || parent.getDirection() != direction) {
                throw new DefinedException(new DefaultErrorCode("400", "上级分类已停用或与当前收支方向不一致"));
            }
            if (parent.getParent() != null) {
                throw new DefinedException(new DefaultErrorCode("400", "业务分类最多支持两级"));
            }
        }
        String normalized = name.trim();
        boolean duplicated = parent == null
                ? repository.existsByCompany_ComIdAndDirectionAndParentIsNullAndName(
                        current.getCompany().getComId(), direction, normalized)
                : repository.existsByCompany_ComIdAndDirectionAndParent_CategoryIdAndName(
                        current.getCompany().getComId(), direction, parent.getCategoryId(), normalized);
        if (duplicated) {
            throw new DefinedException(new DefaultErrorCode("400", "同级分类名称已存在"));
        }
        return categoryContext.saveCustom(current, direction, parent, normalized);
    }
}
