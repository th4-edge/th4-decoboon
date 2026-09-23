package com.th4.edge.decoboon.plan.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 施工阶段验收要求。 */
@Note("阶段验收模式")
public enum StageAcceptanceMode implements Enumerable<String> {
    None("无需验收"),
    ConstructionDone("施工完成"),
    AcceptanceDone("验收完成");

    private final String name;
    StageAcceptanceMode(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
