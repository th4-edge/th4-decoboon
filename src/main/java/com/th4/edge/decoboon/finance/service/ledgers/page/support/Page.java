package com.th4.edge.decoboon.finance.service.ledgers.page.support;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 资金流水分页实现。 */
@Note("资金流水分页")
@Properties(allow = {
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "project", "direction", "category", "categoryNode", "purpose", "source", "sourceId", "amount",
                        "account", "occurredAt", "counterparty", "summary", "remark", "originalLedger",
                        "correctionGroupId", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = LedgerCategory.class,
                properties = {"categoryId", "name", "description", "direction", "code", "builtIn", "sort", "active"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/ledger/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.ledgers.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long ledgerId;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String direction;
    @AutoInject
    private Long categoryId;
    @AutoInject
    private String category;
    /** 待关联业务分类：手工记账且尚未关联分类节点。 */
    @AutoInject
    private Boolean uncategorized;
    @AutoInject
    private LocalDate startDate;
    @AutoInject
    private LocalDate endDate;
    @AutoInject
    private String keyword;
    /** 门店维度（DEC-065）：为空表示不限门店（企业范围）或本店（门店范围）。 */
    @AutoInject
    private Long storeId;

    private final LedgerRepository ledgerRepository;
    private final FinanceAccessContext accessContext;

    public Page(LedgerRepository ledgerRepository, FinanceAccessContext accessContext) {
        this.ledgerRepository = ledgerRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Ledger> call(Ledger request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(
                Sort.Order.desc("occurredAt"),
                Sort.Order.desc("ledgerId")));
        Specification<Ledger> specification = specification(currentEmployee, storeFilter);

        org.springframework.data.domain.Page<Ledger> skeleton = ledgerRepository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }
        List<Long> ids = skeleton.getContent().stream().map(Ledger::getLedgerId).toList();
        Map<Long, Ledger> byId = new LinkedHashMap<>();
        for (Ledger item : ledgerRepository.findDetailsByIdIn(ids)) {
            byId.put(item.getLedgerId(), item);
        }
        List<Ledger> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }

    private Specification<Ledger> specification(Employee currentEmployee, Long storeFilter) {
        Specification<Ledger> specification = accessContext.visible(currentEmployee, Ledger.class);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        return specification.and((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("voided")));
            if (ledgerId != null) {
                predicates.add(cb.equal(root.get("ledgerId"), ledgerId));
            }
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("projectId"), projectId));
            }
            if (!StoreScope.isAllStores(storeFilter)) {
                predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
            }
            if (direction != null && !direction.isBlank()) {
                LedgerDirection parsed = enumValue(LedgerDirection.class, direction);
                predicates.add(parsed == null ? cb.disjunction() : cb.equal(root.get("direction"), parsed));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryNode").get("categoryId"), categoryId));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (Boolean.TRUE.equals(uncategorized)) {
                predicates.add(cb.and(
                        cb.equal(root.get("source"), LedgerSource.Manual),
                        cb.isNull(root.get("categoryNode"))));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), endDate));
            }
            if (!kw.isBlank()) {
                Join<Ledger, LedgerCategory> categoryNode = root.join("categoryNode", JoinType.LEFT);
                String pattern = "%" + kw + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("summary"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("counterparty"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("category"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(categoryNode.get("name"), "")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumType, String rawValue) {
        for (E value : enumType.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(rawValue.trim())) {
                return value;
            }
        }
        return null;
    }
}
