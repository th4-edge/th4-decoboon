package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 企业成员变更动作。 */
@Note("成员变更动作")
public enum MemberLogAction implements Enumerable<String> {
    Join("加入"),
    Resign("离职"),
    Suspend("停用"),
    Resume("恢复"),
    RoleChange("角色调整"),
    StoreChange("门店调整"),
    TeamChange("团队调整"),
    Switch("切换企业"),
    Handover("成员交接");

    private final String name;

    MemberLogAction(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
