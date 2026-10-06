package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.context.FinanceIdempotencyContext;
import com.th4.edge.decoboon.finance.repository.ReceiptRefundRepository;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentStatus;
import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentType;
import com.th4.edge.decoboon.payroll.context.CommissionAdjustmentContext;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollCalculationContext;
import com.th4.edge.decoboon.payroll.model.CommissionAdjustment;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.repository.CommissionAdjustmentRepository;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CommissionAdjustmentContextTest {

    @Test
    void shouldCreateRefundAdjustmentFromExcessReleasedCommission() {
        CommissionAdjustmentRepository adjustmentRepository = mock(CommissionAdjustmentRepository.class);
        CommissionItemRepository commissionRepository = mock(CommissionItemRepository.class);
        ReceiptRepository receiptRepository = mock(ReceiptRepository.class);
        PayrollAccessContext accessContext = mock(PayrollAccessContext.class);
        FinanceIdempotencyContext idempotencyContext = mock(FinanceIdempotencyContext.class);
        CommissionAdjustmentContext context = new CommissionAdjustmentContext(
                adjustmentRepository, commissionRepository, receiptRepository, mock(ReceiptRefundRepository.class),
                new PayrollCalculationContext(), accessContext, idempotencyContext);

        Company company = new Company();
        company.setComId(1L);
        Employee finance = new Employee();
        finance.setEmpId(9L);
        finance.setCompany(company);
        Employee employee = new Employee();
        employee.setEmpId(2L);
        employee.setCompany(company);
        Project project = new Project();
        project.setProjectId(3L);
        CommissionItem commission = new CommissionItem();
        commission.setCommissionId(20L);
        commission.setCompany(company);
        commission.setEmployee(employee);
        commission.setProject(project);
        commission.setContractAmount(new BigDecimal("100000.00"));
        commission.setCommissionAmount(new BigDecimal("3000.00"));
        commission.setReleasedAmount(new BigDecimal("3000.00"));
        commission.setPaidAmount(new BigDecimal("3000.00"));
        Receipt receipt = new Receipt();
        receipt.setAmount(new BigDecimal("90000.00"));

        when(accessContext.requireEditor(finance)).thenReturn(finance);
        when(idempotencyContext.amount(any())).thenAnswer(invocation -> String.valueOf((Object) invocation.getArgument(0)));
        when(idempotencyContext.text(any())).thenAnswer(invocation -> String.valueOf((Object) invocation.getArgument(0)));
        when(idempotencyContext.fingerprint(any())).thenReturn("fingerprint-refund");
        when(idempotencyContext.clientKeyOr(any(), any())).thenReturn("adjustment-refund-key");
        when(adjustmentRepository.findByCompany_ComIdAndIdempotencyKey(1L, "adjustment-refund-key"))
                .thenReturn(Optional.empty());
        when(commissionRepository.findById(20L)).thenReturn(Optional.of(commission));
        when(adjustmentRepository.findByCompany_ComIdAndOriginalCommission_CommissionIdOrderByAdjustmentIdAsc(1L, 20L))
                .thenReturn(List.of());
        when(receiptRepository.findByCompany_ComIdAndProject_ProjectIdOrderByReceivedAtDesc(1L, 3L))
                .thenReturn(List.of(receipt));
        when(adjustmentRepository.saveAndFlush(any(CommissionAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CommissionAdjustment result = context.create(finance, 20L,
                CommissionAdjustmentType.Refund, new BigDecimal("10000.00"), null,
                null, LocalDate.of(2026, 10, 7), "退款负向测试");

        assertThat(result.getDeductionAmount()).isEqualByComparingTo("600.00");
        assertThat(result.getRefundAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.getStatus()).isEqualTo(CommissionAdjustmentStatus.Pending);
    }

    @Test
    void shouldRejectManualAdjustmentAbovePaidAmount() {
        CommissionAdjustmentRepository adjustmentRepository = mock(CommissionAdjustmentRepository.class);
        CommissionItemRepository commissionRepository = mock(CommissionItemRepository.class);
        PayrollAccessContext accessContext = mock(PayrollAccessContext.class);
        FinanceIdempotencyContext idempotencyContext = mock(FinanceIdempotencyContext.class);
        CommissionAdjustmentContext context = new CommissionAdjustmentContext(
                adjustmentRepository, commissionRepository, mock(ReceiptRepository.class), mock(ReceiptRefundRepository.class),
                new PayrollCalculationContext(), accessContext, idempotencyContext);

        Company company = new Company();
        company.setComId(1L);
        Employee finance = new Employee();
        finance.setEmpId(9L);
        finance.setCompany(company);
        Employee employee = new Employee();
        employee.setEmpId(2L);
        employee.setCompany(company);
        CommissionItem commission = new CommissionItem();
        commission.setCommissionId(20L);
        commission.setCompany(company);
        commission.setEmployee(employee);
        commission.setPaidAmount(new BigDecimal("500.00"));

        when(accessContext.requireEditor(finance)).thenReturn(finance);
        when(idempotencyContext.amount(any())).thenAnswer(invocation -> String.valueOf((Object) invocation.getArgument(0)));
        when(idempotencyContext.text(any())).thenAnswer(invocation -> String.valueOf((Object) invocation.getArgument(0)));
        when(idempotencyContext.fingerprint(any())).thenReturn("fingerprint-manual");
        when(idempotencyContext.clientKeyOr(any(), any())).thenReturn("adjustment-manual-key");
        when(adjustmentRepository.findByCompany_ComIdAndIdempotencyKey(1L, "adjustment-manual-key"))
                .thenReturn(Optional.empty());
        when(commissionRepository.findById(20L)).thenReturn(Optional.of(commission));
        when(adjustmentRepository.findByCompany_ComIdAndOriginalCommission_CommissionIdOrderByAdjustmentIdAsc(1L, 20L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> context.create(finance, 20L,
                CommissionAdjustmentType.CrossPeriod, null, new BigDecimal("500.01"),
                null, LocalDate.of(2026, 10, 7), "越界调整"))
                .hasMessageContaining("抵扣金额不能超过");
    }

    @Test
    void shouldCarryPartialAdjustmentIntoNextAllocation() {
        CommissionAdjustmentContext context = new CommissionAdjustmentContext(
                mock(CommissionAdjustmentRepository.class), mock(CommissionItemRepository.class),
                mock(ReceiptRepository.class), mock(ReceiptRefundRepository.class),
                new PayrollCalculationContext(), mock(PayrollAccessContext.class), mock(FinanceIdempotencyContext.class));
        CommissionAdjustment adjustment = new CommissionAdjustment();
        adjustment.setDeductionAmount(new BigDecimal("1000.00"));
        adjustment.setAppliedAmount(BigDecimal.ZERO);
        adjustment.setStatus(CommissionAdjustmentStatus.Pending);

        BigDecimal first = context.apply(List.of(adjustment), new BigDecimal("400.00"));
        assertThat(first).isEqualByComparingTo("400.00");
        assertThat(adjustment.getAppliedAmount()).isEqualByComparingTo("400.00");
        assertThat(adjustment.getStatus()).isEqualTo(CommissionAdjustmentStatus.Pending);

        BigDecimal second = context.apply(List.of(adjustment), new BigDecimal("600.00"));
        assertThat(second).isEqualByComparingTo("600.00");
        assertThat(adjustment.getAppliedAmount()).isEqualByComparingTo("1000.00");
        assertThat(adjustment.getStatus()).isEqualTo(CommissionAdjustmentStatus.Included);
    }
}
