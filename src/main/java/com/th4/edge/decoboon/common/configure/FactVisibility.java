package com.th4.edge.decoboon.common.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 事实节点的最小可见性类别；具体字段仍由场景白名单和角色权限继续裁剪。 */
@Note("事实节点可见性")
public enum FactVisibility implements Enumerable<String> {
    Internal("内部"),
    OwnerVisible("业主可见");

    private final String name;

    FactVisibility(String name) {
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
