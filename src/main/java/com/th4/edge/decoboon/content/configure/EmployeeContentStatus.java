package com.th4.edge.decoboon.content.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 员工内容任务执行状态。 */
@Note("员工内容执行状态")
public enum EmployeeContentStatus implements Enumerable<String> {
    NotStarted("未开始"),
    Drafting("制作中"),
    Rendering("生成中"),
    PendingReview("待审核"),
    ReadyToPublish("可发布"),
    Published("已发布"),
    Backfilled("已回填"),
    Rejected("需修改");

    private final String name;

    EmployeeContentStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
    @Override public String getValue() { return name(); }
}
