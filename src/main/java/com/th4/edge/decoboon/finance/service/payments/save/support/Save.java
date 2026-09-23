package com.th4.edge.decoboon.finance.service.payments.save.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.PayableSettlement;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.PayableSettlementRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
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
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 付款创建实现，付款与应付核销在同一事务内完成。 */
@Note("业务付款创建")
@Properties(allow = {
        @AllowProperties(model = Payment.class,
                properties = {"paymentId", "amount", "account", "paidAt", "payeeName", "worker", "transactionNo",
                        "voucherUrl", "remark", "createTime"}),
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "name", "type", "amount", "paidAmount", "dueDate", "status", "payeeName", "property" }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = Worker.class, properties = {"workerId", "name", "trade", "phone"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/payment/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.payments.save.Save {

    @User
    private Employee employee;
    /** 单张应付付款（与 payableIds 二选一）。 */
    @AutoInject
    private Long payableId;

    /** 多应付核销：一笔付款冲抵同一供应商的多张应付单，按到期日先后自动分摊。 */
    @AutoInject
    private java.util.List<Long> payableIds;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    private FundAccount account;
    @AutoInject
    @NotNull
    private LocalDate paidAt;
    @AutoInject
    private String payeeName;
    @AutoInject
    private String transactionNo;
    @AutoInject
    private String voucherUrl;
    @AutoInject
    private String remark;
    /** 个人工人（工资发放付款时填写，用于生成个人工资凭证）。 */
    @AutoInject
    private Long workerId;

    private final PaymentRepository paymentRepository;
    private final PayableRepository payableRepository;
    private final PayableSettlementRepository payableSettlementRepository;
    private final FinanceAccessContext accessContext;
    private final WorkerRepository workerRepository;

    public Save(PaymentRepository paymentRepository,
                PayableRepository payableRepository,
                PayableSettlementRepository payableSettlementRepository,
                FinanceAccessContext accessContext,
                WorkerRepository workerRepository) {
        this.paymentRepository = paymentRepository;
        this.payableRepository = payableRepository;
        this.payableSettlementRepository = payableSettlementRepository;
        this.accessContext = accessContext;
        this.workerRepository = workerRepository;
    }

    /** 工人档案必须属于当前企业；未选择时返回空（非工资付款）。 */
    private Worker resolveWorker(Employee current, String payeeFallback) {
        if (workerId == null) return null;
        return workerRepository.findByWorkerIdAndCompany_ComId(workerId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工人档案不存在或无权访问")));
    }

    @Override
    @Transactional
    public Payment call(Payment request) {
        Employee current = accessContext.requireEditor(employee);
        if (payableIds != null && !payableIds.isEmpty()) {
            return settleMultiple(request, current, payableIds);
        }
        Payable payable = payableRepository.findById(payableId)
                .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "应付单不存在或无权访问")));
        BigDecimal paid = accessContext.value(amount);
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额必须大于 0"));
        }
        BigDecimal outstanding = accessContext.outstanding(payable.getAmount(), payable.getPaidAmount());
        if (paid.compareTo(outstanding) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额不能超过应付余额"));
        }
        Project project = payable.getProject();
        if (project != null) {
            accessContext.resolveProject(current, project.getProjectId());
        }

        request.setProject(project);
        request.setPayable(payable);
        request.setAmount(paid);
        request.setAccount(account == null ? FundAccount.CompanyBank : account);
        request.setPaidAt(paidAt);
        Worker worker = resolveWorker(current, payable.getPayeeName());
        request.setWorker(worker);
        request.setPayeeName(resolvePayeeName(payeeName, worker, payable.getPayeeName()));
        request.setTransactionNo(transactionNo);
        request.setVoucherUrl(voucherUrl);
        request.setRemark(remark);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        Payment saved = paymentRepository.save(request);

        payable.setPaidAmount(accessContext.value(payable.getPaidAmount()).add(paid));
        accessContext.refreshPayableStatus(payable);
        payableRepository.save(payable);
        accessContext.recordLedger(current, project, LedgerDirection.Expense,
                payable.getType().getName(), LedgerSource.Payment, saved.getPaymentId(), paid,
                saved.getAccount(), paidAt, saved.getPayeeName(),
                "付款 · " + payable.getName(), remark);
        return saved;
    }

    /** 收款人姓名：显式传入 > 工人姓名 > 应付单收款方。 */
    private String resolvePayeeName(String input, Worker worker, String fallback) {
        if (input != null && !input.isBlank()) return input;
        if (worker != null && worker.getName() != null && !worker.getName().isBlank()) return worker.getName();
        return fallback;
    }

    /**
     * 一笔付款冲抵多张应付单：按到期日先后自动分摊，写入核销明细并回写应付状态。
     */
    private Payment settleMultiple(Payment request, Employee current, java.util.List<Long> ids) {
        BigDecimal paid = accessContext.value(amount);
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额必须大于 0"));
        }
        Long comId = current.getCompany().getComId();
        java.util.List<Payable> payables = payableRepository.findAllById(ids).stream()
                .filter(item -> item.getCompany() != null && comId.equals(item.getCompany().getComId()))
                .sorted(java.util.Comparator.comparing(Payable::getDueDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        if (payables.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("404", "应付单不存在或无权访问"));
        }
        BigDecimal total = payables.stream()
                .map(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(total) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额不能超过所选应付单的待付合计"));
        }
        Payable primary = payables.get(0);
        Project project = primary.getProject();
        if (project != null) {
            accessContext.resolveProject(current, project.getProjectId());
        }
        request.setProject(project);
        request.setPayable(primary);
        request.setPartner(primary.getPayeePartner());
        request.setAmount(paid);
        request.setAccount(account == null ? FundAccount.CompanyBank : account);
        request.setPaidAt(paidAt);
        Worker worker = resolveWorker(current, primary.getPayeeName());
        request.setWorker(worker);
        request.setPayeeName(resolvePayeeName(payeeName, worker, primary.getPayeeName()));
        request.setTransactionNo(transactionNo);
        request.setVoucherUrl(voucherUrl);
        request.setRemark(remark);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        Payment saved = paymentRepository.save(request);

        BigDecimal remaining = paid;
        for (Payable payable : payables) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal outstanding = accessContext.outstanding(payable.getAmount(), payable.getPaidAmount());
            BigDecimal allocate = remaining.min(outstanding);
            PayableSettlement settlement = new PayableSettlement();
            settlement.setCompany(current.getCompany());
            settlement.setStore(current.getStore());
            settlement.setTeam(current.getTeam());
            settlement.setOwner(current);
            settlement.setPayment(saved);
            settlement.setPayable(payable);
            settlement.setAmount(allocate);
            settlement.setRemark(remark);
            payableSettlementRepository.save(settlement);

            payable.setPaidAmount(accessContext.value(payable.getPaidAmount()).add(allocate));
            accessContext.refreshPayableStatus(payable);
            payableRepository.save(payable);
            remaining = remaining.subtract(allocate);
        }
        accessContext.recordLedger(current, project, LedgerDirection.Expense,
                primary.getType().getName(), LedgerSource.Payment, saved.getPaymentId(), paid,
                saved.getAccount(), paidAt, saved.getPayeeName(),
                "付款 · " + primary.getName() + "（核销 " + payables.size() + " 张应付）", remark);
        return saved;
    }
}
