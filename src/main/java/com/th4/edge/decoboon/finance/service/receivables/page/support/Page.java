package com.th4.edge.decoboon.finance.service.receivables.page.support;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.model.Project;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 应收计划分页实现。 */
@Note("应收计划分页")
@Properties(allow = {
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "project", "property", "customer", "name", "stageName", "stageCode", "triggerType", "type", "amount",
                        "receivedAmount", "dueDate", "status", "invoiceRequired", "invoiceNo", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "siteAddr", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "role"})
})
@Service("finance/receivable/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.receivables.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String type;
    @AutoInject
    private String status;
    @AutoInject
    private String keyword;
    /** 门店维度（DEC-065）：为空表示不限门店（企业范围）或本店（门店范围）。 */
    @AutoInject
    private Long storeId;

    private final ReceivableRepository receivableRepository;
    private final FinanceAccessContext accessContext;

    public Page(ReceivableRepository receivableRepository, FinanceAccessContext accessContext) {
        this.receivableRepository = receivableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Receivable> call(Receivable request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(
                Sort.Order.asc("dueDate"),
                Sort.Order.desc("receivableId")));
        Specification<Receivable> specification = specification(currentEmployee, storeFilter);

        org.springframework.data.domain.Page<Receivable> skeleton = receivableRepository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }
        List<Long> ids = skeleton.getContent().stream().map(Receivable::getReceivableId).toList();
        Map<Long, Receivable> byId = new LinkedHashMap<>();
        for (Receivable item : receivableRepository.findDetailsByIdIn(ids)) {
            byId.put(item.getReceivableId(), item);
        }
        List<Receivable> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }

    private Specification<Receivable> specification(Employee currentEmployee, Long storeFilter) {
        Specification<Receivable> specification = accessContext.visible(currentEmployee, Receivable.class);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        return specification.and((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("projectId"), projectId));
            }
            if (!StoreScope.isAllStores(storeFilter)) {
                predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
            }
            if (type != null && !type.isBlank()) {
                ReceivableType parsed = enumValue(ReceivableType.class, type);
                predicates.add(parsed == null ? cb.disjunction() : cb.equal(root.get("type"), parsed));
            }
            if (status != null && !status.isBlank()) {
                ReceivableStatus parsed = enumValue(ReceivableStatus.class, status);
                predicates.add(parsed == null ? cb.disjunction() : cb.equal(root.get("status"), parsed));
            }
            if (!kw.isBlank()) {
                String pattern = "%" + kw + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("project").get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("project").get("no"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("stageName"), "")), pattern)));
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
