package com.th4.edge.decoboon.finance.service.payables.page.support;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.supply.model.Partner;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 应付分页实现。 */
@Note("应付分页")
@Properties(allow = {
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "project", "materialPlan", "name", "type", "payeeEmployee", "payeePartner", "payeeName",
                        "period", "amount", "estimated", "settledAmount", "paidAmount", "dueDate", "status", "urgent", "sourceNo", "remark",
                        "createdBy", "createTime", "property" }),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.MaterialPlan.class,
                properties = {"materialPlanId", "name", "quantity", "unit", "cost", "status", "requiredAt"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Partner.class,
                properties = {"partnerId", "name", "type", "telNo", "settlementMode", "settlementDays", "cutoffDay"})
})
@Service("finance/payable/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.payables.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long payableId;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String type;
    @AutoInject
    private String status;
    @AutoInject
    private Boolean urgent;
    /** 服务端语义筛选：actionable / overdue / dueSoon / urgent / salary。 */
    @AutoInject
    private String filter;

    /** 按供应商筛选（供应链台账联动）。 */
    @AutoInject
    private Long partnerId;
    @AutoInject
    private String keyword;
    /** 门店维度（DEC-065）：为空表示不限门店（企业范围）或本店（门店范围）。 */
    @AutoInject
    private Long storeId;

    private final PayableRepository payableRepository;
    private final FinanceAccessContext accessContext;

    public Page(PayableRepository payableRepository, FinanceAccessContext accessContext) {
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Payable> call(Payable request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(
                Sort.Order.asc("dueDate"),
                Sort.Order.desc("payableId")));
        Specification<Payable> specification = specification(currentEmployee, storeFilter);

        org.springframework.data.domain.Page<Payable> skeleton = payableRepository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }
        List<Long> ids = skeleton.getContent().stream().map(Payable::getPayableId).toList();
        Map<Long, Payable> byId = new LinkedHashMap<>();
        for (Payable item : payableRepository.findDetailsByIdIn(ids)) {
            byId.put(item.getPayableId(), item);
        }
        List<Payable> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }

    private Specification<Payable> specification(Employee currentEmployee, Long storeFilter) {
        Specification<Payable> specification = accessContext.visible(currentEmployee, Payable.class);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        return specification.and((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (payableId != null) {
                predicates.add(cb.equal(root.get("payableId"), payableId));
            }
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("projectId"), projectId));
            }
            if (!StoreScope.isAllStores(storeFilter)) {
                predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
            }
            if (partnerId != null) {
                predicates.add(cb.equal(root.get("payeePartner").get("partnerId"), partnerId));
            }
            if (type != null && !type.isBlank()) {
                PayableType parsed = enumValue(PayableType.class, type);
                predicates.add(parsed == null ? cb.disjunction() : cb.equal(root.get("type"), parsed));
            }
            if (status != null && !status.isBlank()) {
                PayableStatus parsed = enumValue(PayableStatus.class, status);
                predicates.add(parsed == null ? cb.disjunction() : cb.equal(root.get("status"), parsed));
            }
            if (urgent != null) {
                predicates.add(cb.equal(root.get("urgent"), urgent));
            }
            appendSemanticFilter(predicates, root, cb);
            if (!kw.isBlank()) {
                Join<Payable, Project> project = root.join("project", JoinType.LEFT);
                String pattern = "%" + kw + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("payeeName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(project.get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(project.get("no"), "")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }

    private void appendSemanticFilter(List<Predicate> predicates,
                                      jakarta.persistence.criteria.Root<Payable> root,
                                      jakarta.persistence.criteria.CriteriaBuilder cb) {
        if (filter == null || filter.isBlank()) return;
        String value = filter.trim().toLowerCase();
        LocalDate today = LocalDate.now();
        Predicate outstanding = cb.lessThan(root.<BigDecimal>get("paidAmount"), root.<BigDecimal>get("amount"));
        Predicate notClosed = cb.notEqual(root.get("status"), PayableStatus.Closed);
        switch (value) {
            case "actionable" -> predicates.add(cb.and(outstanding, notClosed));
            case "overdue" -> predicates.add(cb.and(outstanding, notClosed,
                    cb.lessThan(root.<LocalDate>get("dueDate"), today)));
            case "duesoon" -> predicates.add(cb.and(outstanding, notClosed,
                    cb.between(root.<LocalDate>get("dueDate"), today, today.plusDays(7))));
            case "urgent" -> predicates.add(cb.and(outstanding, notClosed,
                    cb.isTrue(root.get("urgent"))));
            case "salary" -> predicates.add(cb.and(outstanding, notClosed,
                    cb.equal(root.get("type"), PayableType.Salary)));
            default -> predicates.add(cb.disjunction());
        }
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
