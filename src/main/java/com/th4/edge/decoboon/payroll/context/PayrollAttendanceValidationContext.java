package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 月度考勤天数的统一校验，保存与确认必须使用同一口径。 */
@Component
public class PayrollAttendanceValidationContext {

    public Integer integerScheduledDays(BigDecimal value) {
        if (value == null) {
            throw invalid("月应出勤天数必须为 1 至 31 天的整数");
        }
        try {
            int days = value.intValueExact();
            if (days < 1 || days > 31) {
                throw invalid("月应出勤天数必须为 1 至 31 天的整数");
            }
            return days;
        } catch (ArithmeticException exception) {
            throw invalid("月应出勤天数必须为 1 至 31 天的整数");
        }
    }

    public void validate(AttendanceSummary summary) {
        if (summary == null) {
            throw invalid("考勤汇总不能为空");
        }
        Integer scheduled = summary.getScheduledDays();
        if (scheduled == null || scheduled < 1 || scheduled > 31) {
            throw invalid("月应出勤天数必须为 1 至 31 天的整数");
        }
        BigDecimal scheduledDays = BigDecimal.valueOf(scheduled);
        BigDecimal recordedDays = roundedDays(summary);
        if (recordedDays.compareTo(scheduledDays) > 0) {
            throw invalid("正常出勤、请假、旷工、外勤、年假、调休合计不能超过应出勤天数");
        }
    }

    public void requireComplete(AttendanceSummary summary) {
        validate(summary);
        BigDecimal recordedDays = roundedDays(summary);
        if (recordedDays.compareTo(BigDecimal.valueOf(summary.getScheduledDays())) != 0) {
            throw invalid("确认考勤前，正常出勤、请假、旷工、外勤、年假、调休合计必须等于应出勤天数");
        }
    }

    private BigDecimal roundedDays(AttendanceSummary summary) {
        return value(summary.getNormalDays())
                .add(value(summary.getPersonalLeaveDays()))
                .add(value(summary.getSickLeaveDays()))
                .add(value(summary.getAbsenceDays()))
                .add(value(summary.getExternalDays()))
                .add(value(summary.getAnnualLeaveDays()))
                .add(value(summary.getCompLeaveDays()));
    }

    private BigDecimal value(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() > 2) {
            throw invalid("考勤天数最多保留两位小数");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(31)) > 0) {
            throw invalid("考勤天数必须为 0 至 31 天，可填写小数");
        }
        return value;
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}
