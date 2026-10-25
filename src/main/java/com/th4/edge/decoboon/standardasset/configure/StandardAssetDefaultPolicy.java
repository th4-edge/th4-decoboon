package com.th4.edge.decoboon.standardasset.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 标准资产默认项策略。 */
@Note("标准资产默认项策略")
public enum StandardAssetDefaultPolicy implements Enumerable<String> {
    NotApplicable("不适用"),
    Deferred("待用户确认，本期不实现"),
    OptionalSingle("可选，同一范围内最多一个");

    private final String name;

    StandardAssetDefaultPolicy(String name) {
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
