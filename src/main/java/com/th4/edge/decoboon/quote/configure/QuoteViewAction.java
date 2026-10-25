package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价场景允许暴露的动作类别；具体角色能力仍由既有权限矩阵决定。 */
@Note("报价场景动作")
public enum QuoteViewAction implements Enumerable<String> {
    VIEW("查看"),
    SAVE("保存"),
    SUBMIT("提交"),
    REVISE("改版"),
    ADJUST("调整"),
    ACCEPT("登记接受"),
    REJECT("驳回"),
    CREATE_CONTRACT("发起合同"),
    PRINT("打印"),
    EXPAND_SOURCE("展开来源"),
    NAVIGATE("跳转统一视图"),
    GENERATE("生成归档"),
    REGENERATE("重新生成归档"),
    OPEN_ARCHIVE("打开归档");

    private final String name;

    QuoteViewAction(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
