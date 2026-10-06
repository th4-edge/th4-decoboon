package com.th4.edge.decoboon.payroll.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("AttendanceStatus")
public enum AttendanceStatus implements Enumerable<String> {

    Draft("草稿"),
    Pending("待确认"),
    Confirmed("已确认"),
    InPayroll("已入工资单"),
    Disputed("申诉中"),
    Rejected("已退回"),
    ;

    private final String name;

    AttendanceStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
