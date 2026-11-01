package com.th4.edge.decoboon.finance.service.expensecategories.page.support;

import com.th4.edge.decoboon.finance.context.ExpenseAccessContext;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.repository.ExpenseCategoryRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 当前企业费用分类选择器分页。 */
@Note("费用分类选择器分页")
@Properties(allow = @AllowProperties(model = ExpenseCategory.class,
        properties = {"categoryId", "code", "name", "description", "builtIn", "active", "sort"}))
@Service("expense/categories/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.expensecategories.page.Page {
    @User private Employee employee;
    @AutoInject private Long categoryId;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private String keyword;
    @AutoInject private Boolean includeInactive;
    private final ExpenseAccessContext accessContext;
    private final ExpenseCategoryRepository repository;

    public Page(ExpenseAccessContext accessContext, ExpenseCategoryRepository repository) {
        this.accessContext = accessContext;
        this.repository = repository;
    }

    @Override
    public org.springframework.data.domain.Page<ExpenseCategory> call(ExpenseCategory request) {
        Employee currentEmployee = accessContext.requireSubmitter(employee);
        Long comId = currentEmployee.getCompany().getComId();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        Specification<ExpenseCategory> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("company").get("comId"), comId));
            if (categoryId != null) {
                predicates.add(builder.equal(root.get("categoryId"), categoryId));
            }
            if (!Boolean.TRUE.equals(includeInactive)) {
                predicates.add(builder.isTrue(root.get("active")));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("code")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        org.springframework.data.domain.Page<ExpenseCategory> result = repository.findAll(
                specification,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "sort").and(Sort.by(Sort.Direction.ASC, "categoryId")))
        );
        return new PageImpl<>(result.getContent(), PageRequest.of(page, pageSize), result.getTotalElements());
    }
}
