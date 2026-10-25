package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** 已批准费用到应付、付款状态投影与付清关闭。 */
@Component
public class ExpensePaymentContext {

    public static final String SOURCE_TYPE = "Expense";

    private final PayableRepository payableRepository;
    private final ExpenseRepository expenseRepository;

    public ExpensePaymentContext(PayableRepository payableRepository, ExpenseRepository expenseRepository) {
        this.payableRepository = payableRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public Payable ensurePayable(Expense expense, Employee current) {
        if (expense.getStatus() != ExpenseStatus.Approved) {
            throw new DefinedException(new DefaultErrorCode("409", "只有已批准费用可以生成应付"));
        }
        if (expense.getPayable() != null) {
            return expense.getPayable();
        }
        Payable existing = payableRepository.findByCompany_ComIdAndSourceTypeAndSourceId(
                expense.getCompany().getComId(), SOURCE_TYPE, expense.getExpenseId()).orElse(null);
        if (existing != null) {
            expense.setPayable(existing);
            expenseRepository.save(expense);
            return existing;
        }
        Payable payable = new Payable();
        payable.setCompany(expense.getCompany());
        payable.setStore(expense.getStore());
        payable.setTeam(expense.getTeam());
        payable.setOwner(expense.getOwner());
        payable.setProject(expense.getProject());
        payable.setName("费用报销 · " + expense.getExpenseNo());
        payable.setType(PayableType.Expense);
        payable.setPayeeEmployee(expense.getPayeeEmployee());
        payable.setPayeeName(expense.getPayeeEmployee() == null ? null : expense.getPayeeEmployee().getName());
        payable.setPeriod(expense.getExpenseDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        payable.setAmount(expense.getAmount());
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setEstimated(Boolean.FALSE);
        payable.setDueDate(expense.getExpenseDate());
        payable.setStatus(PayableStatus.Pending);
        payable.setUrgent(Boolean.FALSE);
        payable.setSourceNo(expense.getExpenseNo());
        payable.setSourceType(SOURCE_TYPE);
        payable.setSourceId(expense.getExpenseId());
        payable.setRemark(expense.getDescription());
        payable.setCreatedBy(current);
        Payable saved;
        try {
            saved = payableRepository.saveAndFlush(payable);
        } catch (DataIntegrityViolationException exception) {
            saved = payableRepository.findByCompany_ComIdAndSourceTypeAndSourceId(
                            expense.getCompany().getComId(), SOURCE_TYPE, expense.getExpenseId())
                    .orElseThrow(() -> exception);
        }
        expense.setPayable(saved);
        expenseRepository.save(expense);
        return saved;
    }

    public Expense requirePaymentEligible(Payable payable) {
        if (payable == null || !SOURCE_TYPE.equals(payable.getSourceType()) || payable.getSourceId() == null) {
            return null;
        }
        Expense expense = expenseRepository.findByCompany_ComIdAndExpenseId(
                        payable.getCompany().getComId(), payable.getSourceId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "费用单不存在或无权访问")));
        if (expense.getStatus() != ExpenseStatus.Approved) {
            throw new DefinedException(new DefaultErrorCode("409", "费用未审批通过，不能付款"));
        }
        return expense;
    }

    @Transactional
    public void closeIfPaid(Payable payable) {
        if (payable == null || !SOURCE_TYPE.equals(payable.getSourceType()) || payable.getSourceId() == null) return;
        Expense expense = expenseRepository.findByCompany_ComIdAndExpenseId(
                payable.getCompany().getComId(), payable.getSourceId()).orElse(null);
        if (expense == null) return;
        expense.setPayable(payable);
        if (payable.getStatus() == PayableStatus.Paid && expense.getStatus() == ExpenseStatus.Approved) {
            expense.setStatus(ExpenseStatus.Closed);
        }
        expenseRepository.save(expense);
    }

    public Expense applyProjection(Expense expense) {
        Payable payable = expense.getPayable();
        if (payable == null) {
            expense.setPaymentStatus(null);
            expense.setPaidAmount(BigDecimal.ZERO);
            expense.setUnpaidAmount(expense.getAmount() == null ? BigDecimal.ZERO : expense.getAmount());
            return expense;
        }
        BigDecimal amount = value(payable.getAmount());
        BigDecimal paid = value(payable.getPaidAmount());
        expense.setPaymentStatus(payable.getStatus() == null ? null : payable.getStatus().name());
        expense.setPaidAmount(paid);
        expense.setUnpaidAmount(amount.subtract(paid).max(BigDecimal.ZERO));
        if (payable.getStatus() == PayableStatus.Paid && expense.getStatus() == ExpenseStatus.Approved) {
            expense.setStatus(ExpenseStatus.Closed);
        }
        return expense;
    }

    public boolean canPay(Expense expense, Employee current) {
        if (expense == null || current == null || expense.getPayable() == null) return false;
        if (expense.getStatus() != ExpenseStatus.Approved && expense.getStatus() != ExpenseStatus.Closed) return false;
        BigDecimal outstanding = value(expense.getPayable().getAmount())
                .subtract(value(expense.getPayable().getPaidAmount()));
        if (outstanding.signum() <= 0) return false;
        boolean financeRole = current.getRole() == com.th4.edge.tenant.user.configure.EmployeeRole.Boss
                || current.getRole() == com.th4.edge.tenant.user.configure.EmployeeRole.StoreManager
                || current.getRole() == com.th4.edge.tenant.user.configure.EmployeeRole.Finance;
        if (!financeRole) return false;
        boolean self = expense.getOwner() != null && Objects.equals(expense.getOwner().getEmpId(), current.getEmpId());
        return !self || current.getRole() == com.th4.edge.tenant.user.configure.EmployeeRole.Boss;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
