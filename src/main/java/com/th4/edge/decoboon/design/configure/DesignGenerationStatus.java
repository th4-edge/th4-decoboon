package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** AI设计生成任务状态。 */
@Note("AI设计生成任务状态")
public enum DesignGenerationStatus implements Enumerable<String> {
    ContextBuilding("构建上下文"),
    PromptReady("提示词就绪"),
    Generating("生成中"),
    DraftReady("草稿就绪"),
    DesignerConfirmed("设计师已确认"),
    Shared("已分享"),
    FeedbackReceived("已收到反馈"),
    Revised("已修版"),
    Locked("已锁定"),
    Failed("生成失败");

    private final String name;

    DesignGenerationStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return toString();
    }
}
