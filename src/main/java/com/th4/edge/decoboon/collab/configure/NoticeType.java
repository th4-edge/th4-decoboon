package com.th4.edge.decoboon.collab.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

@Note("通知类型")
public enum NoticeType implements Enumerable<String> {
    Customer("客户提醒"),
    Quote("报价提醒"),
    Project("项目提醒"),
    Task("任务提醒"),
    Overdue("逾期提醒"),
    System("系统通知");

    private final String name;

    NoticeType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
