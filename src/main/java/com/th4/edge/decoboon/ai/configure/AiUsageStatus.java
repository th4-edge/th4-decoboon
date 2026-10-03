package com.th4.edge.decoboon.ai.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** AI 调用状态。 */
@Note("AI调用状态")
public enum AiUsageStatus implements Enumerable<String> {

    Requested("已请求"),
    Validated("已校验"),
    Reserved("已预占"),
    Running("执行中"),
    Succeeded("成功"),
    Failed("失败"),
    Cancelled("已取消"),
    Expired("已超时"),
    DuplicateBlocked("重复拦截");

    private final String name;

    AiUsageStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean validCall() {
        return this == Validated || this == Reserved || this == Running || this == Succeeded;
    }

    public boolean success() {
        return this == Succeeded;
    }

    public boolean failure() {
        return this == Failed || this == Expired;
    }
}
