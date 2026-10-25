package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.FinanceIdempotencyContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.PayableSettlement;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PayableSettlementRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.payroll.configure.PayrollItemStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.configure.PayslipStatus;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 员工工资/提成发放：核销应付、写资金流水并在发放成功后发布工资条。 */
@Component
public class EmployeePaymentContext {

    private final FinanceAccessContext financeAccess;
    private final PayrollAccessContext payrollAccessContext;
    private final FinanceIdempotencyContext idempotencyContext;
    private final PaymentRepository paymentRepository;
    private final PayableRepository payableRepository;
    private final PayableSettlementRepository settlementRepository;
    private final CommissionItemRepository commissionItemRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final PayrollSheetRepository sheetRepository;
    private final PayslipRepository payslipRepository;

    public EmployeePaymentContext(FinanceAccessContext financeAccess,
                                  PayrollAccessContext payrollAccessContext,
                                  FinanceIdempotencyContext idempotencyContext,
                                  PaymentRepository paymentRepository,
                                  PayableRepository payableRepository,
                                  PayableSettlementRepository settlementRepository,
                                  CommissionItemRepository commissionItemRepository,
                                  PayrollItemRepository payrollItemRepository,
                                  PayrollSheetRepository sheetRepository,
                                  PayslipRepository payslipRepository) {
        this.financeAccess = financeAccess;
        this.payrollAccessContext = payrollAccessContext;
        this.idempotencyContext = idempotencyContext;
        this.paymentRepository = paymentRepository;
        this.payableRepository = payableRepository;
        this.settlementRepository = settlementRepository;
        this.commissionItemRepository = commissionItemRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.sheetRepository = sheetRepository;
        this.payslipRepository = payslipRepository;
    }

    @Transactional
    public Payment pay(Employee principal, List<Long> payableIds, BigDecimal amount,
                       FundAccount account, LocalDate paidAt, String batchNo,
                       String idempotencyKey, String remark) {
        // DEC-093：员工工资/提成发放仅 BOSS、财务；店长即使具备通用财务写权限也不得发放。
        Employee current = payrollAccessContext.requireEditor(principal);
        if (amount == null || amount.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "发放金额必须大于 0"));
        }
        List<Long> ids = payableIds == null ? List.of() : payableIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("404", "员工应付单不存在或无权访问"));
        }
        Long comId = current.getCompany().getComId();
        Specification<Payable> requested = (root, query, builder) -> root.get("payableId").in(ids);
        List<Payable> payables = payableRepository.findAll(
                requested.and(financeAccess.visible(current, Payable.class)));
        if (payables.size() != ids.size()) {
            throw new DefinedException(new DefaultErrorCode("404", "员工应付单不存在或无权访问"));
        }
        Employee payee = payables.get(0).getPayeeEmployee();
        if (payee == null || payables.stream().anyMatch(item -> item.getPayeeEmployee() == null
                || !item.getPayeeEmployee().getEmpId().equals(payee.getEmpId()))) {
            throw new DefinedException(new DefaultErrorCode("400", "同一发放批次只能对应同一员工"));
        }
        if (payables.stream().anyMatch(item -> item.getType() != PayableType.EmployeeSalary
                && item.getType() != PayableType.EmployeeCommission)) {
            throw new DefinedException(new DefaultErrorCode("400", "只能发放员工工资或员工提成应付"));
        }
        Long projectId = payables.get(0).getProject() == null ? null : payables.get(0).getProject().getProjectId();
        if (payables.stream().anyMatch(item -> {
            Long currentProject = item.getProject() == null ? null : item.getProject().getProjectId();
            return !Objects.equals(projectId, currentProject);
        })) {
            throw new DefinedException(new DefaultErrorCode("400", "同一发放批次不能跨项目"));
        }
        Long sourceStoreId = storeId(payables.get(0));
        if (payables.stream().anyMatch(item -> !Objects.equals(sourceStoreId, storeId(item)))) {
            throw new DefinedException(new DefaultErrorCode("400", "同一发放批次只能对应同一门店"));
        }

        String fingerprintBase = String.join("|",
                "EMPLOYEE_PAYMENT", payee.getEmpId().toString(), ids.toString(),
                idempotencyContext.amount(amount), account == null ? FundAccount.CompanyBank.name() : account.name(),
                paidAt == null ? "" : paidAt.toString(), idempotencyContext.text(batchNo), idempotencyContext.text(remark));
        String fingerprint = idempotencyContext.fingerprint(fingerprintBase);
        String key = idempotencyContext.clientKeyOr(idempotencyKey, fingerprint);
        Optional<Payment> existing = paymentRepository.findByCompany_ComIdAndIdempotencyKey(comId, key);
        if (existing.isPresent()) {
            Payment found = existing.get();
            if (fingerprint.equals(found.getIdempotencyFingerprint())) {
                return found;
            }
            throw idempotencyContext.conflict();
        }

        BigDecimal outstanding = payables.stream()
                .map(this::paymentOutstanding)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.compareTo(outstanding) > 0) {
            String message = payables.stream().allMatch(item -> item.getType() == PayableType.EmployeeCommission)
                    ? "发放金额不能超过已释放且未发放的提成金额"
                    : "发放金额不能超过员工应付未付金额";
            throw new DefinedException(new DefaultErrorCode("400", message));
        }

        Payment payment = new Payment();
        payment.setCompany(current.getCompany());
        financeAccess.applySourceOwnership(payment, payables.get(0));
        payment.setProject(payables.get(0).getProject());
        payment.setPayable(payables.get(0));
        payment.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        payment.setAccount(account == null ? FundAccount.CompanyBank : account);
        payment.setPaidAt(paidAt == null ? LocalDate.now() : paidAt);
        payment.setPayeeName(payee.getName());
        payment.setPayeeEmployee(payee);
        payment.setBatchNo(batchNo);
        payment.setIdempotencyKey(key);
        payment.setIdempotencyFingerprint(fingerprint);
        payment.setRemark(remark);
        payment.setCreatedBy(current);
        Payment saved = paymentRepository.saveAndFlush(payment);

        BigDecimal remaining = amount;
        for (Payable payable : payables) {
            if (remaining.signum() <= 0) break;
            BigDecimal payableOutstanding = paymentOutstanding(payable);
            BigDecimal allocated = remaining.min(payableOutstanding);
            PayableSettlement settlement = new PayableSettlement();
            settlement.setCompany(current.getCompany());
            settlement.setStore(saved.getStore());
            settlement.setTeam(saved.getTeam());
            settlement.setOwner(saved.getOwner());
            settlement.setPayment(saved);
            settlement.setPayable(payable);
            settlement.setAmount(allocated);
            settlement.setRemark("员工发放");
            settlementRepository.save(settlement);
            payable.setPaidAmount(financeAccess.value(payable.getPaidAmount()).add(allocated));
            financeAccess.refreshPayableStatus(payable);
            payableRepository.save(payable);
            if ("CommissionItem".equals(payable.getSourceType()) && payable.getSourceId() != null) {
                commissionItemRepository.findById(payable.getSourceId()).ifPresent(commission -> {
                    commission.setPaidAmount(financeAccess.value(commission.getPaidAmount()).add(allocated));
                    if (commission.getPaidAmount().compareTo(financeAccess.value(commission.getCommissionAmount())) >= 0) {
                        commission.setStatus(com.th4.edge.decoboon.payroll.configure.CommissionStatus.Paid);
                    } else {
                        commission.setStatus(com.th4.edge.decoboon.payroll.configure.CommissionStatus.InPayment);
                    }
                    commissionItemRepository.save(commission);
                });
            }
            remaining = remaining.subtract(allocated);
        }
        financeAccess.recordLedger(current, saved.getProject(), LedgerDirection.Expense,
                "员工工资/提成", LedgerSource.Payment, saved.getPaymentId(), amount, saved.getAccount(),
                saved.getPaidAt(), payee.getName(), "员工工资/提成发放", remark,
                saved.getStore(), saved.getTeam(), saved.getOwner());
        publishPayslipsAfterPayment(current, saved, payables);
        return saved;
    }

    private Long storeId(Payable payable) {
        return payable == null || payable.getStore() == null ? null : payable.getStore().getStoreId();
    }

    private BigDecimal paymentOutstanding(Payable payable) {
        BigDecimal payableOutstanding = financeAccess.outstanding(payable.getAmount(), payable.getPaidAmount());
        if (payable.getType() != PayableType.EmployeeCommission
                || !"CommissionItem".equals(payable.getSourceType())
                || payable.getSourceId() == null) {
            return payableOutstanding;
        }
        return commissionItemRepository.findById(payable.getSourceId())
                .map(commission -> financeAccess.outstanding(
                        commission.getReleasedAmount() == null ? BigDecimal.ZERO : commission.getReleasedAmount(),
                        commission.getPaidAmount()))
                .orElse(BigDecimal.ZERO)
                .min(payableOutstanding);
    }

    private void publishPayslipsAfterPayment(Employee current, Payment payment, List<Payable> payables) {
        for (Payable payable : payables) {
            if (!"PayrollItem".equals(payable.getSourceType()) || payable.getSourceId() == null) continue;
            PayrollItem item = payrollItemRepository.findById(payable.getSourceId()).orElse(null);
            if (item == null) continue;
            PayrollSheet sheet = item.getSheet();
            List<PayrollItem> sheetItems = payrollItemRepository
                    .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(
                            current.getCompany().getComId(), sheet.getSheetId());
            boolean employeePaid = sheetItems.stream()
                    .filter(row -> row.getEmployee().getEmpId().equals(item.getEmployee().getEmpId()))
                    .allMatch(row -> payableRepository.findByCompany_ComIdAndSourceTypeAndSourceId(
                                    current.getCompany().getComId(), "PayrollItem", row.getItemId())
                            .map(value -> value.getStatus() == PayableStatus.Paid)
                            .orElse(false));
            if (!employeePaid) continue;
            Payslip payslip = payslipRepository.findByCompany_ComIdAndSheet_SheetIdAndEmployee_EmpId(
                    current.getCompany().getComId(), sheet.getSheetId(), item.getEmployee().getEmpId()).orElse(null);
            if (payslip != null && payslip.getPublishedAt() == null) {
                payslip.setStatus(PayslipStatus.Published);
                payslip.setPublishedAt(LocalDateTime.now());
                payslipRepository.save(payslip);
            }
            boolean sheetPaid = sheetItems.stream()
                    .allMatch(row -> payableRepository.findByCompany_ComIdAndSourceTypeAndSourceId(
                                    current.getCompany().getComId(), "PayrollItem", row.getItemId())
                            .map(value -> value.getStatus() == PayableStatus.Paid)
                            .orElse(false));
            if (sheetPaid) {
                sheet.setStatus(PayrollSheetStatus.Paid);
                sheetRepository.save(sheet);
            } else if (sheet.getStatus() == PayrollSheetStatus.Confirmed) {
                sheet.setStatus(PayrollSheetStatus.PartialPaid);
                sheetRepository.save(sheet);
            }
        }
    }
}
