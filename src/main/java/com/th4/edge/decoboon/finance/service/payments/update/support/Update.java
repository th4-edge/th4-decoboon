package com.th4.edge.decoboon.finance.service.payments.update.support;

import com.th4.edge.decoboon.document.context.VoucherContext;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.labor.repository.WorkerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 工资付款补录关联工人。
 *
 * <p>只允许修改 Payment.worker；金额、账户、日期、应付核销和台账均不在本 Provider 的写入范围。
 * 已生成工资凭证时按方案 A 使用原凭证号重渲染 PDF；补录审计表和前后值留痕需产品确认后再落 DDL。</p>
 */
@Note("工资付款补录关联工人")
@Properties(allow = {
        @AllowProperties(model = Payment.class,
                properties = {"paymentId", "project", "payable", "amount", "account", "paidAt", "payeeName",
                        "worker", "transactionNo", "voucherUrl", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status"}),
        @AllowProperties(model = Worker.class, properties = {"workerId", "name", "trade", "phone"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class, properties = {"custId", "name", "telNo", "community", "addr", "area", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.Payable.class, properties = {"payableId", "name", "type", "amount", "paidAmount", "dueDate", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class, properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"})
})
@Service("finance/payment/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.finance.service.payments.update.Update {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long paymentId;
    @AutoInject
    @NotNull
    private Long workerId;

    private final PaymentRepository paymentRepository;
    private final WorkerRepository workerRepository;
    private final FinanceAccessContext accessContext;
    private final DataScopeContext dataScopeContext;
    private final VoucherContext voucherContext;

    public Update(PaymentRepository paymentRepository,
                  WorkerRepository workerRepository,
                  FinanceAccessContext accessContext,
                  DataScopeContext dataScopeContext,
                  VoucherContext voucherContext) {
        this.paymentRepository = paymentRepository;
        this.workerRepository = workerRepository;
        this.accessContext = accessContext;
        this.dataScopeContext = dataScopeContext;
        this.voucherContext = voucherContext;
    }

    @Override
    @Transactional
    public Payment call(Payment request) {
        Employee current = accessContext.requireEditor(employee);
        Long comId = current.getCompany().getComId();
        Specification<Payment> byId = (root, query, builder) -> builder.equal(root.get("paymentId"), paymentId);
        Payment payment = paymentRepository.findOne(
                        byId.and(DataScopeSpecification.visible(Payment.class, dataScopeContext.resolve(current))))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "付款记录不存在或无权访问")));
        if (payment.getPayable() == null || payment.getPayable().getType() != PayableType.Salary) {
            throw new DefinedException(new DefaultErrorCode("400", "只有工资付款可以补录发放工人"));
        }
        Worker worker = workerRepository.findById(workerId)
                .filter(item -> item.getCompany() != null && comId.equals(item.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工人档案不存在或无权访问")));
        if (payment.getWorker() != null) {
            if (payment.getWorker().getWorkerId().equals(workerId)) {
                return payment;
            }
            throw new DefinedException(new DefaultErrorCode("400", "该付款已关联工人，不允许替换"));
        }
        payment.setWorker(worker);
        Payment saved = paymentRepository.save(payment);
        voucherContext.rebuildForPayment(current, saved.getPaymentId());
        return saved;
    }
}
