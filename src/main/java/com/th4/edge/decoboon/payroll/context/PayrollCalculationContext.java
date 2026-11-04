package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 员工工资与提成金额计算上下文。 */
@Component
public class PayrollCalculationContext {

    public static final BigDecimal DEFAULT_PERSONAL_LEAVE_FACTOR = new BigDecimal("1.0");
    public static final BigDecimal DEFAULT_SICK_LEAVE_FACTOR = new BigDecimal("0.5");
    public static final BigDecimal DEFAULT_ABSENCE_FACTOR = new BigDecimal("2.0");

    public BigDecimal dailySalary(BigDecimal baseSalary, Integer scheduledDays) {
        BigDecimal base = money(baseSalary);
        if (scheduledDays == null || scheduledDays <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "薪酬方案或当月考勤的应出勤天数必须大于 0"));
        }
        return base.divide(BigDecimal.valueOf(scheduledDays), 6, RoundingMode.HALF_UP);
    }

    public BigDecimal attendanceDeduction(BigDecimal dailySalary,
                                          AttendanceSummary summary,
                                          BigDecimal personalLeaveFactor,
                                          BigDecimal sickLeaveFactor,
                                          BigDecimal absenceFactor) {
        if (summary == null) {
            throw new DefinedException(new DefaultErrorCode("400", "考勤汇总不能为空"));
        }
        BigDecimal daily = money(dailySalary);
        return daily.multiply(value(summary.getPersonalLeaveDays()))
                .multiply(factor(personalLeaveFactor, DEFAULT_PERSONAL_LEAVE_FACTOR))
                .add(daily.multiply(value(summary.getSickLeaveDays()))
                        .multiply(factor(sickLeaveFactor, DEFAULT_SICK_LEAVE_FACTOR)))
                .add(daily.multiply(value(summary.getAbsenceDays()))
                        .multiply(factor(absenceFactor, DEFAULT_ABSENCE_FACTOR)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void calculate(PayrollItem item) {
        BigDecimal gross = money(item.getBaseSalaryAmount())
                .add(money(item.getCommissionAmount()))
                .add(money(item.getBonusAmount()))
                .add(money(item.getAllowanceAmount()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal deduction = money(item.getAttendanceDeduction())
                .add(money(item.getSocialInsuranceAmount()))
                .add(money(item.getTaxAmount()))
                .add(money(item.getOtherDeduction()))
                .setScale(2, RoundingMode.HALF_UP);
        item.setGrossAmount(gross);
        item.setDeductionAmount(deduction);
        item.setNetAmount(gross.subtract(deduction).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * 本次应释放提成 = 累计应释放 − 已释放。
     * 累计应释放 = min(提成金额, 提成金额 × 累计有效回款 ÷ 主合同签约额)。
     */
    public BigDecimal commissionReleaseAmount(CommissionItem item, BigDecimal cumulativeEffectiveReceipt) {
        BigDecimal cumulativeRelease = commissionCumulativeRelease(item, cumulativeEffectiveReceipt);
        BigDecimal alreadyReleased = money(item.getReleasedAmount());
        BigDecimal delta = cumulativeRelease.subtract(alreadyReleased);
        return delta.signum() <= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : delta.setScale(2, RoundingMode.HALF_UP);
    }

    /** 累计应释放 = min(提成金额, 提成金额 × 累计有效回款 ÷ 主合同签约额)。 */
    public BigDecimal commissionCumulativeRelease(CommissionItem item, BigDecimal cumulativeEffectiveReceipt) {
        if (item == null) {
            throw new DefinedException(new DefaultErrorCode("400", "提成明细不能为空"));
        }
        BigDecimal commission = money(item.getCommissionAmount());
        BigDecimal contractAmount = money(item.getContractAmount());
        BigDecimal received = money(cumulativeEffectiveReceipt);
        if (commission.signum() <= 0 || contractAmount.signum() <= 0 || received.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal receiptRatio = received.divide(contractAmount, 8, RoundingMode.HALF_UP);
        BigDecimal cappedRatio = receiptRatio.min(BigDecimal.ONE);
        return commission.multiply(cappedRatio).min(commission).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal factor(BigDecimal configured, BigDecimal fallback) {
        return configured == null ? fallback : configured;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
