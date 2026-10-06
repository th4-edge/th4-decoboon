package com.th4.edge.decoboon.finance.service.payments.page.support;
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
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 付款分页实现。 */
@Note("付款分页")
@Properties(allow = {
        @AllowProperties(model = Payment.class,
                properties = {"paymentId", "project", "payable", "amount", "account", "paidAt", "payeeName",
                        "worker", "transactionNo", "voucherUrl", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "name", "type", "amount", "paidAmount", "dueDate", "status", "payeeName", "property" }),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
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

    private final PaymentRepository paymentRepository;
    private final FinanceAccessContext accessContext;

    public Page(PaymentRepository paymentRepository, FinanceAccessContext accessContext) {
        this.paymentRepository = paymentRepository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<Payment> call(Payment request) {
        Employee currentEmployee = accessContext.requireReader(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Payment> items = paymentRepository.findAll(accessContext.visible(currentEmployee, Payment.class))
                .stream()
                .filter(item -> projectId == null || (item.getProject() != null && projectId.equals(item.getProject().getProjectId())))
                .filter(item -> kw.isBlank()
                        || (item.getPayeeName() != null && item.getPayeeName().toLowerCase().contains(kw))
                        || (item.getPayable().getName() != null && item.getPayable().getName().toLowerCase().contains(kw))
                        || (item.getTransactionNo() != null && item.getTransactionNo().toLowerCase().contains(kw)))
                .sorted(Comparator.comparing(Payment::getPaidAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
