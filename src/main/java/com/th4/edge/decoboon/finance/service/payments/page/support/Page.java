package com.th4.edge.decoboon.finance.service.payments.page.support;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 付款分页实现。 */
@Note("付款分页")
@Properties(allow = {
        @AllowProperties(model = Payment.class,
                properties = {"paymentId", "project", "payable", "amount", "account", "paidAt", "payeeName",
                        "worker", "transactionNo", "voucherUrl", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "name", "type", "amount", "paidAmount", "dueDate", "status", "payeeName", "property" }),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "manager", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.labor.model.Worker.class,
                properties = {"workerId", "name", "trade", "phone"})
})
@Service("finance/payment/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.payments.page.Page {

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

    private final PaymentRepository paymentRepository;
    private final FinanceAccessContext accessContext;

    public Page(PaymentRepository paymentRepository, FinanceAccessContext accessContext) {
        this.paymentRepository = paymentRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Payment> call(Payment request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        Long storeFilter = accessContext.resolveStoreFilter(currentEmployee, storeId);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(
                Sort.Order.desc("paidAt"),
                Sort.Order.desc("paymentId")));
        Specification<Payment> specification = specification(currentEmployee, storeFilter);

        org.springframework.data.domain.Page<Payment> skeleton = paymentRepository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }

        List<Long> ids = skeleton.getContent().stream().map(Payment::getPaymentId).toList();
        Map<Long, Payment> byId = new LinkedHashMap<>();
        for (Payment item : paymentRepository.findDetailsByIdIn(ids)) {
            byId.put(item.getPaymentId(), item);
        }
        List<Payment> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }

    private Specification<Payment> specification(Employee currentEmployee, Long storeFilter) {
        Specification<Payment> specification = accessContext.visible(currentEmployee, Payment.class);
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
                Join<Payment, Project> project = root.join("project", JoinType.LEFT);
                String pattern = "%" + kw + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.<String>get("payeeName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("payable").get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("transactionNo"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("remark"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(project.get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(project.get("no"), "")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        });
    }
}
