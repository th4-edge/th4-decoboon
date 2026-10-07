package com.th4.edge.decoboon.finance.service.ledgercategories.update.support;

import com.th4.edge.decoboon.finance.configure.LedgerDirection;
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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 企业业务分类维护实现：自定义分类可维护，内置二级分类也可按企业改名/停用。 */
@Note("维护企业记账分类")
@Properties(allow = @AllowProperties(model = LedgerCategory.class,
        properties = {"categoryId", "name", "description", "direction", "builtIn", "sort", "active", "children"}))
@Service("finance/ledger-category/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.ledgercategories.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long categoryId;
    @AutoInject
    @NotEmpty
    @Length(max = 64)
    private String name;
    @AutoInject
    @NotEmpty
    @Length(max = 500)
    private String description;
    @AutoInject
    @NotNull
    private Boolean active;

    private final LedgerCategoryRepository repository;
    private final FinanceAccessContext accessContext;

    public Update(LedgerCategoryRepository repository, FinanceAccessContext accessContext) {
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
        // 旧两级模板的历史一级分组仍有启用子分类，不允许在本轮扁平化口径下直接编辑或停用。
        boolean legacyGroup = Boolean.TRUE.equals(category.getBuiltIn())
                && category.getParent() == null
                && repository.existsByCompany_ComIdAndParent_CategoryIdAndActiveTrue(
                        current.getCompany().getComId(), category.getCategoryId());
        if (legacyGroup) {
            throw new DefinedException(new DefaultErrorCode("400", "旧两级分类分组不可直接修改或停用"));
        }
        String normalized = name.trim();
        validateNameUnique(category, normalized);
        category.setName(normalized);
        category.setDescription(description.trim());
        category.setActive(active);
        if (!active && category.getParent() == null) {
            List<LedgerCategory> children = repository.findByCompany_ComIdAndParent_CategoryId(
                    current.getCompany().getComId(), category.getCategoryId());
            children.forEach(item -> item.setActive(false));
            repository.saveAll(children);
        }
        return repository.save(category);
    }

    private void validateNameUnique(LedgerCategory category, String name) {
        Long comId = category.getCompany().getComId();
        LedgerDirection direction = category.getDirection();
        boolean duplicated;
        if (category.getParent() == null) {
            duplicated = repository.existsByCompany_ComIdAndDirectionAndParentIsNullAndNameAndCategoryIdNot(
                    comId, direction, name, category.getCategoryId());
        } else {
            Long parentId = category.getParent().getCategoryId();
            duplicated = repository
                    .findByCompany_ComIdAndParent_CategoryIdAndDirectionAndActiveTrueOrderBySortAscCategoryIdAsc(
                            comId, parentId, direction)
                    .stream()
                    .anyMatch(item -> !item.getCategoryId().equals(category.getCategoryId())
                            && item.getName().equals(name));
        }
        if (duplicated) {
            throw new DefinedException(new DefaultErrorCode("400", "同级分类名称已存在"));
        }
    }
}
