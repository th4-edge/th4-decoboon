package com.th4.edge.decoboon.payroll;

import com.th4.edge.decoboon.payroll.context.PayrollCalculationContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollCalculationContextTest {

    private final PayrollCalculationContext context = new PayrollCalculationContext();

    @Test
    void shouldCalculateDailySalaryAndAttendanceDeduction() {
        BigDecimal daily = context.dailySalary(new BigDecimal("6000.00"), 30);
        AttendanceSummary summary = new AttendanceSummary();
        summary.setPersonalLeaveDays(new BigDecimal("1"));
        summary.setSickLeaveDays(new BigDecimal("2"));
        summary.setAbsenceDays(new BigDecimal("1"));

        BigDecimal deduction = context.attendanceDeduction(daily, summary, null, null, null);

        assertThat(daily).isEqualByComparingTo("200.000000");
        assertThat(deduction).isEqualByComparingTo("800.00");
    }

    @Test
    void shouldCalculateHalfDayAttendanceDeductionWithoutTruncation() {
        BigDecimal daily = context.dailySalary(new BigDecimal("6000.00"), 30);
        AttendanceSummary summary = new AttendanceSummary();
        summary.setPersonalLeaveDays(new BigDecimal("0.5"));
        summary.setSickLeaveDays(new BigDecimal("0.5"));

        BigDecimal deduction = context.attendanceDeduction(daily, summary, null, null, null);

        assertThat(deduction).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldCalculateGrossDeductionAndNet() {
        PayrollItem item = new PayrollItem();
        item.setBaseSalaryAmount(new BigDecimal("6000.00"));
        item.setAttendanceDeduction(new BigDecimal("700.00"));
        item.setCommissionAmount(new BigDecimal("800.00"));
        item.setBonusAmount(new BigDecimal("200.00"));
        item.setAllowanceAmount(new BigDecimal("100.00"));
        item.setSocialInsuranceAmount(new BigDecimal("300.00"));
        item.setTaxAmount(new BigDecimal("50.00"));
        item.setOtherDeduction(new BigDecimal("20.00"));

        context.calculate(item);

        assertThat(item.getGrossAmount()).isEqualByComparingTo("7100.00");
        assertThat(item.getDeductionAmount()).isEqualByComparingTo("1070.00");
        assertThat(item.getNetAmount()).isEqualByComparingTo("6030.00");
    }

    @Test
    void shouldReleaseCommissionByReceiptRatio() {
        CommissionItem item = new CommissionItem();
        item.setContractAmount(new BigDecimal("200000.00"));
        item.setCommissionAmount(new BigDecimal("4000.00"));
        item.setReleasedAmount(new BigDecimal("1000.00"));

        BigDecimal release = context.commissionReleaseAmount(item, new BigDecimal("100000.00"));

        assertThat(release).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldCapCommissionReleaseAtCommissionAmount() {
        CommissionItem item = new CommissionItem();
        item.setContractAmount(new BigDecimal("100000.00"));
        item.setCommissionAmount(new BigDecimal("3000.00"));
        item.setReleasedAmount(new BigDecimal("2500.00"));

        BigDecimal release = context.commissionReleaseAmount(item, new BigDecimal("120000.00"));

        assertThat(release).isEqualByComparingTo("500.00");
    }
}
