package com.th4.edge.decoboon.finance.service.refunds.page.support;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.repository.ReceiptRefundRepository;
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

@Note("客户退款分页")
@Properties(allow = {
        @AllowProperties(model = ReceiptRefund.class,
                properties = {"refundId", "project", "receipt", "customer", "amount", "account", "refundAt", "reason", "createTime"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Receipt.class, properties = {"receiptId", "amount", "receivedAt", "payerName"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"})
})
@Service("finance/refund/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.refunds.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    /** 门店维度（`DEC-065` / `P1-a2`）：不传 / 0 = 全部门店；门店范围角色恒锁本店，传他店 403。 */
    @AutoInject
    private Long storeId;

    private final ReceiptRefundRepository repository;
    private final FinanceAccessContext accessContext;

    public Page(ReceiptRefundRepository repository, FinanceAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReceiptRefund> call(ReceiptRefund request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(
                Sort.Order.desc("refundAt"),
                Sort.Order.desc("refundId")));

        Specification<ReceiptRefund> specification = accessContext.visible(currentEmployee, ReceiptRefund.class)
                .and((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (projectId != null) {
                        predicates.add(cb.equal(root.get("project").get("projectId"), projectId));
                    }
                    if (!StoreScope.isAllStores(storeFilter)) {
                        predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
                    }
                    return cb.and(predicates.toArray(Predicate[]::new));
                });

        org.springframework.data.domain.Page<ReceiptRefund> skeleton = repository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }
        List<Long> ids = skeleton.getContent().stream().map(ReceiptRefund::getRefundId).toList();
        Map<Long, ReceiptRefund> byId = new LinkedHashMap<>();
        for (ReceiptRefund item : repository.findByRefundIdIn(ids)) {
            byId.put(item.getRefundId(), item);
        }
        List<ReceiptRefund> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }
}
