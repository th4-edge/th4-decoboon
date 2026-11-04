package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

/** 月度考勤天数的统一校验，保存与确认必须使用同一口径。 */
@Component
public class PayrollAttendanceValidationContext {

    public void validate(AttendanceSummary summary) {
        if (summary == null) {
            throw invalid("考勤汇总不能为空");
        }
        Integer scheduled = summary.getScheduledDays();
        if (scheduled == null || scheduled < 1 || scheduled > 31) {
            throw invalid("月应出勤天数必须为 1 至 31 天的整数");
        }
        int scheduledDays = scheduled;
        int recordedDays = value(summary.getNormalDays())
                + value(summary.getPersonalLeaveDays())
                + value(summary.getSickLeaveDays())
                + value(summary.getAbsenceDays())
                + value(summary.getExternalDays())
                + value(summary.getAnnualLeaveDays())
                + value(summary.getCompLeaveDays());
        if (recordedDays > scheduledDays) {
            throw invalid("正常出勤、请假、旷工、外勤、年假、调休合计不能超过应出勤天数");
        }
    }

    public void requireComplete(AttendanceSummary summary) {
        validate(summary);
        int recordedDays = value(summary.getNormalDays())
                + value(summary.getPersonalLeaveDays())
                + value(summary.getSickLeaveDays())
                + value(summary.getAbsenceDays())
                + value(summary.getExternalDays())
                + value(summary.getAnnualLeaveDays())
                + value(summary.getCompLeaveDays());
        if (recordedDays != summary.getScheduledDays()) {
            throw invalid("确认考勤前，正常出勤、请假、旷工、外勤、年假、调休合计必须等于应出勤天数");
        }
    }

    private int value(Integer value) {
        if (value == null) return 0;
        if (value < 0 || value > 31) {
            throw invalid("考勤天数必须为 0 至 31 天的整数");
        }
        return value;
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}
