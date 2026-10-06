package com.th4.edge.decoboon.finance.service.payments.save.support;

import com.th4.edge.decoboon.carryover.configure.CarryoverBusinessType;
import com.th4.edge.decoboon.carryover.context.CarryoverLockContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceIdempotencyContext;
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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 付款创建实现，付款、应付核销和资金台账在同一事务内完成，并按客户端提交意图防重。 */
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
    private List<Long> payableIds;

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

    /** 客户端在进入提交动作时生成，失败重试必须复用同一个值。 */
    @AutoInject
    @Length(max = 128)
    private String idempotencyKey;

    private final PaymentRepository paymentRepository;
    private final PayableRepository payableRepository;
    private final PayableSettlementRepository payableSettlementRepository;
    private final FinanceAccessContext accessContext;
    private final FinanceIdempotencyContext idempotencyContext;
    private final CarryoverLockContext carryoverLockContext;
    private final WorkerRepository workerRepository;

    public Save(PaymentRepository paymentRepository,
                PayableRepository payableRepository,
                PayableSettlementRepository payableSettlementRepository,
                FinanceAccessContext accessContext,
                FinanceIdempotencyContext idempotencyContext,
                CarryoverLockContext carryoverLockContext,
                WorkerRepository workerRepository) {
        this.paymentRepository = paymentRepository;
        this.payableRepository = payableRepository;
        this.payableSettlementRepository = payableSettlementRepository;
        this.accessContext = accessContext;
        this.idempotencyContext = idempotencyContext;
        this.carryoverLockContext = carryoverLockContext;
        this.workerRepository = workerRepository;
    }

    @Override
    @Transactional
    public Payment call(Payment request) {
        Employee current = accessContext.requireEditor(employee);
        if (payableIds != null && !payableIds.isEmpty()) {
            return settleMultiple(request, current);
        }
        return settleSingle(request, current);
    }

    private Payment settleSingle(Payment request, Employee current) {
        Payable payable = payableRepository.findById(payableId)
                .filter(item -> item.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "应付单不存在或无权访问")));
        BigDecimal paid = accessContext.value(amount);
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额必须大于 0"));
        }
        FundAccount targetAccount = account == null ? FundAccount.CompanyBank : account;
        Project project = payable.getProject();
        if (project != null) {
            accessContext.resolveProject(current, project.getProjectId());
        }
        Worker worker = resolveWorker(current);
        String targetPayee = resolvePayeeName(payeeName, worker, payable.getPayeeName());
        List<Long> targetIds = List.of(payable.getPayableId());
        String fingerprintBase = fingerprintBase("SINGLE", targetIds, paid, targetAccount, paidAt,
                targetPayee, workerId, transactionNo);
        String fingerprint = idempotencyContext.fingerprint(fingerprintBase);
        String key = idempotencyContext.clientKeyOr(idempotencyKey, fingerprint);
        Optional<Payment> existing = paymentRepository
                .findByCompany_ComIdAndIdempotencyKey(current.getCompany().getComId(), key);
        if (existing.isPresent()) {
            Payment found = existing.get();
            boolean sameFingerprint = found.getIdempotencyFingerprint() != null
                    && found.getIdempotencyFingerprint().equals(fingerprint);
            boolean legacySameBusiness = found.getIdempotencyFingerprint() == null
                    && sameBusiness(found, "SINGLE", targetIds, paid, targetAccount, paidAt,
                    targetPayee, workerId, transactionNo);
            if (sameFingerprint || legacySameBusiness) {
                return found;
            }
            throw idempotencyContext.conflict();
        }

        // 非重复提交才校验余额，保证重复提交在核销后仍能返回原付款。
        BigDecimal outstanding = accessContext.outstanding(payable.getAmount(), payable.getPaidAmount());
        if (paid.compareTo(outstanding) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额不能超过应付余额"));
        }

        request.setProject(project);
        request.setPayable(payable);
        request.setAmount(paid);
        request.setAccount(targetAccount);
        request.setPaidAt(paidAt);
        request.setWorker(worker);
        request.setPayeeName(targetPayee);
        request.setTransactionNo(transactionNo);
        request.setVoucherUrl(voucherUrl);
        request.setRemark(remark);
        request.setIdempotencyKey(key);
        request.setIdempotencyFingerprint(fingerprint);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        Payment saved = saveIdempotently(request);

        payable.setPaidAmount(accessContext.value(payable.getPaidAmount()).add(paid));
        accessContext.refreshPayableStatus(payable);
        payableRepository.save(payable);
        accessContext.recordLedger(current, project, LedgerDirection.Expense,
                payable.getType().getName(), LedgerSource.Payment, saved.getPaymentId(), paid,
                saved.getAccount(), paidAt, saved.getPayeeName(),
                "付款 · " + payable.getName(), remark);
        if (carryoverLockContext != null) carryoverLockContext.lock(project, CarryoverBusinessType.Payment, current);
        return saved;
    }

    /**
     * 一笔付款冲抵多张应付单：按到期日先后自动分摊，写入核销明细并回写应付状态。
     */
    private Payment settleMultiple(Payment request, Employee current) {
        BigDecimal paid = accessContext.value(amount);
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额必须大于 0"));
        }
        List<Long> requestedIds = payableIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (requestedIds.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("404", "应付单不存在或无权访问"));
        }
        Long comId = current.getCompany().getComId();
        List<Payable> foundPayables = payableRepository.findAllById(requestedIds).stream()
                .filter(item -> item.getCompany() != null && comId.equals(item.getCompany().getComId()))
                .toList();
        if (foundPayables.size() != requestedIds.size()) {
            throw new DefinedException(new DefaultErrorCode("404", "应付单不存在或无权访问"));
        }
        List<Payable> payables = foundPayables.stream()
                .sorted(Comparator.comparing(Payable::getDueDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Payable::getPayableId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Payable primary = payables.get(0);
        Project project = primary.getProject();
        if (project != null) {
            accessContext.resolveProject(current, project.getProjectId());
        }
        FundAccount targetAccount = account == null ? FundAccount.CompanyBank : account;
        Worker worker = resolveWorker(current);
        String targetPayee = resolvePayeeName(payeeName, worker, primary.getPayeeName());
        String fingerprintBase = fingerprintBase("MULTI", requestedIds, paid, targetAccount, paidAt,
                targetPayee, workerId, transactionNo);
        String fingerprint = idempotencyContext.fingerprint(fingerprintBase);
        String key = idempotencyContext.clientKeyOr(idempotencyKey, fingerprint);
        Optional<Payment> existing = paymentRepository
                .findByCompany_ComIdAndIdempotencyKey(comId, key);
        if (existing.isPresent()) {
            Payment found = existing.get();
            boolean sameFingerprint = found.getIdempotencyFingerprint() != null
                    && found.getIdempotencyFingerprint().equals(fingerprint);
            boolean legacySameBusiness = found.getIdempotencyFingerprint() == null
                    && sameBusiness(found, "MULTI", requestedIds, paid, targetAccount, paidAt,
                    targetPayee, workerId, transactionNo);
            if (sameFingerprint || legacySameBusiness) {
                return found;
            }
            throw idempotencyContext.conflict();
        }

        // 非重复提交才校验余额，重复提交不因应付已核销而失败。
        BigDecimal total = payables.stream()
                .map(item -> accessContext.outstanding(item.getAmount(), item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(total) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "付款金额不能超过所选应付单的待付合计"));
        }

        request.setProject(project);
        request.setPayable(primary);
        request.setPartner(primary.getPayeePartner());
        request.setAmount(paid);
        request.setAccount(targetAccount);
        request.setPaidAt(paidAt);
        request.setWorker(worker);
        request.setPayeeName(targetPayee);
        request.setTransactionNo(transactionNo);
        request.setVoucherUrl(voucherUrl);
        request.setRemark(remark);
        request.setIdempotencyKey(key);
        request.setIdempotencyFingerprint(fingerprint);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        Payment saved = saveIdempotently(request);

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
        if (carryoverLockContext != null) carryoverLockContext.lock(project, CarryoverBusinessType.Payment, current);
        return saved;
    }

    /** 先 flush 触发数据库唯一约束，再执行应付回写和台账；并发输家不会产生第二套副作用。 */
    private Payment saveIdempotently(Payment request) {
        try {
            return paymentRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyContext.isDuplicateKey(exception)) {
                throw idempotencyContext.conflict();
            }
            throw exception;
        }
    }

    /** 工人档案必须属于当前企业；未选择时返回空（非工资付款）。 */
    private Worker resolveWorker(Employee current) {
        if (workerId == null) return null;
        return workerRepository.findByWorkerIdAndCompany_ComId(workerId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工人档案不存在或无权访问")));
    }

    /** 收款人姓名：显式传入 > 工人姓名 > 应付单收款方。 */
    private String resolvePayeeName(String input, Worker worker, String fallback) {
        if (input != null && !input.isBlank()) return input;
        if (worker != null && worker.getName() != null && !worker.getName().isBlank()) return worker.getName();
        return fallback;
    }

    /** 单张与多张应付共用的业务指纹；多应付按 ID 排序，保证重试不受请求顺序影响。 */
    private String fingerprintBase(String mode, List<Long> ids, BigDecimal amount, FundAccount account,
                                   LocalDate paidAt, String payeeName, Long workerId, String transactionNo) {
        String canonicalIds = ids.stream().map(String::valueOf).sorted().reduce((left, right) -> left + "," + right).orElse("");
        return String.join("|",
                "PAYMENT",
                mode,
                canonicalIds,
                idempotencyContext.amount(amount),
                account.name(),
                paidAt == null ? "" : paidAt.toString(),
                idempotencyContext.text(payeeName),
                workerId == null ? "" : String.valueOf(workerId),
                idempotencyContext.text(transactionNo));
    }

    /** 旧数据没有指纹列时的兼容比对；新数据始终走指纹列。 */
    private boolean sameBusiness(Payment found, String mode, List<Long> ids, BigDecimal amount,
                                 FundAccount account, LocalDate paidAt, String payeeName,
                                 Long workerId, String transactionNo) {
        if (found.getAmount() == null || found.getAmount().compareTo(amount) != 0
                || found.getAccount() != account
                || paidAt == null || !paidAt.equals(found.getPaidAt())
                || !Objects.equals(payeeName, found.getPayeeName())
                || !Objects.equals(workerId, found.getWorker() == null ? null : found.getWorker().getWorkerId())
                || !Objects.equals(idempotencyContext.text(transactionNo), idempotencyContext.text(found.getTransactionNo()))) {
            return false;
        }
        List<Long> settledIds = payableSettlementRepository.findByPayment_PaymentId(found.getPaymentId()).stream()
                .map(item -> item.getPayable() == null ? null : item.getPayable().getPayableId())
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if ("MULTI".equals(mode)) {
            return settledIds.equals(ids);
        }
        return settledIds.isEmpty()
                && found.getPayable() != null
                && ids.size() == 1
                && ids.get(0).equals(found.getPayable().getPayableId());
    }
}
