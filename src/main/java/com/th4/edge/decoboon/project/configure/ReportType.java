package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 工地动态类型。 */
@Note("工地动态类型")
public enum ReportType implements Enumerable<String> {
    StageOpen("阶段开工"),
    TaskDone("任务完成"),
    Progress("施工进展"),
    AcceptRequest("申请验收"),
    AcceptDone("验收完成"),
    Issue("现场问题"),
    Material("材料到场"),
    Checkin("现场打卡"),
    Notice("项目通知");

    private final String name;

    ReportType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
