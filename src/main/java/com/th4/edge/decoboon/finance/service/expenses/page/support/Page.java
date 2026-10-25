package com.th4.edge.decoboon.finance.service.expenses.page.support;

import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.context.ExpenseAccessContext;
import com.th4.edge.decoboon.finance.context.ExpenseProjectionContext;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 费用/报销分页实现。 */
@Note("费用报销分页")
@Properties(allow = {
        @AllowProperties(model = Expense.class, properties = {
                "expenseId", "expenseNo", "paymentMode", "amount", "expenseDate", "status", "version",
                "submittedAt", "approvedAt", "category", "project", "store", "requester", "owner",
                "payeeEmployee", "approvedBy", "payable", "paymentStatus", "paidAmount", "unpaidAmount", "allowedActions", "nextAction", "createTime", "updateTime"
        }),
        @AllowProperties(model = ExpenseCategory.class,
                properties = {"categoryId", "name", "builtIn", "active"}),
        @AllowProperties(model = Payable.class, properties = {
                "payableId", "name", "type", "amount", "paidAmount", "dueDate", "status", "payeeEmployee", "payeeName"
        }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name", "defaultStore", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("expense/expenses/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.expenses.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private String status;
    @AutoInject
    private String keyword;
    @AutoInject
    private Long projectId;
    @AutoInject
    private Long payeeEmployeeId;
    @AutoInject
    private Long storeId;

    private final ExpenseRepository repository;
    private final ExpenseAccessContext accessContext;
    private final ExpenseProjectionContext projectionContext;

    public Page(ExpenseRepository repository, ExpenseAccessContext accessContext,
                ExpenseProjectionContext projectionContext) {
        this.repository = repository;
        this.accessContext = accessContext;
        this.projectionContext = projectionContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Expense> call(Expense request) {
        Employee currentEmployee = accessContext.requireSubmitter(employee);
        int pageIndex = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(
                Sort.Order.desc("createTime.dateTime"), Sort.Order.desc("expenseId")));
        Specification<Expense> specification = specification(currentEmployee);
        org.springframework.data.domain.Page<Expense> skeleton = repository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }
        List<Long> ids = skeleton.getContent().stream().map(Expense::getExpenseId).toList();
        Map<Long, Expense> details = new LinkedHashMap<>();
        for (Expense item : repository.findPageByIdIn(ids)) {
            details.put(item.getExpenseId(), item);
        }
        repository.findPagePeopleByIdIn(ids);
        List<Expense> ordered = ids.stream().map(details::get).filter(Objects::nonNull)
                .map(item -> projectionContext.apply(item, currentEmployee)).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }

    private Specification<Expense> specification(Employee currentEmployee) {
        Specification<Expense> visible = accessContext.visible(currentEmployee);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return visible.and((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("status"), ExpenseStatus.valueOf(status.trim())));
                } catch (IllegalArgumentException ex) {
                    predicates.add(cb.disjunction());
                }
            }
            if (projectId != null && projectId > 0) {
                predicates.add(cb.equal(root.get("project").get("projectId"), projectId));
            }
            if (payeeEmployeeId != null && payeeEmployeeId > 0) {
                predicates.add(cb.equal(root.get("payeeEmployee").get("empId"), payeeEmployeeId));
            }
            if (storeId != null && storeId > 0) {
                predicates.add(cb.equal(root.get("store").get("storeId"), storeId));
            }
            if (!normalizedKeyword.isBlank()) {
                String pattern = "%" + normalizedKeyword + "%";
                predicates.add(cb.like(cb.lower(root.get("expenseNo")), pattern));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }
}
