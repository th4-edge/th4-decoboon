package com.th4.edge.decoboon.finance.service.receipts.page.support;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
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

/** 收款分页实现。 */
@Note("收款分页")
@Properties(allow = {
        @AllowProperties(model = Receipt.class,
                properties = {"receiptId", "project", "receivable", "customer", "amount", "account", "receivedAt",
                        "payerName", "voucherUrl", "remark", "createdBy", "status", "reversedAt",
                        "reversedBy", "reversalReason", "createTime"}),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "name", "type", "amount", "receivedAmount", "dueDate", "status", "property" }),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/receipt/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.receipts.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String keyword;
    /** 门店维度（DEC-065）：为空表示不限门店（企业范围）或本店（门店范围）。 */
    @AutoInject
    private Long storeId;

    private final ReceiptRepository receiptRepository;
    private final FinanceAccessContext accessContext;

    public Page(ReceiptRepository receiptRepository, FinanceAccessContext accessContext) {
        this.receiptRepository = receiptRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Receipt> call(Receipt request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(
                Sort.Order.desc("receivedAt"),
                Sort.Order.desc("receiptId")));
        Specification<Receipt> specification = specification(currentEmployee, storeFilter);

        org.springframework.data.domain.Page<Receipt> skeleton = receiptRepository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }

        List<Long> ids = skeleton.getContent().stream().map(Receipt::getReceiptId).toList();
        Map<Long, Receipt> byId = new LinkedHashMap<>();
        for (Receipt item : receiptRepository.findDetailsByIdIn(ids)) {
            byId.put(item.getReceiptId(), item);
        }
        List<Receipt> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }

    private Specification<Receipt> specification(Employee currentEmployee, Long storeFilter) {
        Specification<Receipt> specification = accessContext.visible(currentEmployee, Receipt.class);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        return specification.and((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("projectId"), projectId));
            }
            if (!StoreScope.isAllStores(storeFilter)) {
                predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
            }
            if (!kw.isBlank()) {
                String pattern = "%" + kw + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("project").get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("project").get("no"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("payerName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("remark"), "")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }
}
