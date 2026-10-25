package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.document.context.VoucherContext;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.document.repository.VoucherRecordRepository;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.List;

/** 费用详情读取付款与已发行付款凭证摘要；查询范围由费用详情外层权限保证。 */
@Component
public class ExpenseVoucherProjectionContext {

    private final PaymentRepository paymentRepository;
    private final VoucherRecordRepository voucherRecordRepository;
    private final VoucherContext voucherContext;

    public ExpenseVoucherProjectionContext(PaymentRepository paymentRepository,
                                           VoucherRecordRepository voucherRecordRepository,
                                           VoucherContext voucherContext) {
        this.paymentRepository = paymentRepository;
        this.voucherRecordRepository = voucherRecordRepository;
        this.voucherContext = voucherContext;
    }

    public Expense apply(Expense expense, Employee current) {
        if (expense == null || expense.getPayable() == null || current == null) return expense;
        Long comId = current.getCompany().getComId();
        List<Payment> payments = paymentRepository
                .findByCompany_ComIdAndPayable_PayableIdOrderByPaidAtAsc(comId, expense.getPayable().getPayableId());
        List<VoucherRecord> vouchers = voucherRecordRepository
                .findByCompany_ComIdAndSourceTypeAndSourceIdOrderByVoucherIdDesc(
                        comId, VoucherRecord.SOURCE_EXPENSE, expense.getExpenseId());
        vouchers.forEach(voucher -> {
            voucher.getItems().stream()
                    .filter(item -> item.getSourceType() == DocumentType.Payment)
                    .map(item -> item.getSourceId())
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .ifPresent(voucher::setPaymentId);
            voucherContext.signed(voucher);
        });
        expense.setPayments(payments);
        expense.setVouchers(vouchers);
        return expense;
    }
}
