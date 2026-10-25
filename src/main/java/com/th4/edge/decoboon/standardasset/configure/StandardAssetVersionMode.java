package com.th4.edge.decoboon.standardasset.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 标准资产历史追溯方式。 */
@Note("标准资产版本模式")
public enum StandardAssetVersionMode implements Enumerable<String> {
    Revision("修订"),
    ImmutableVersion("不可变版本");

    private final String name;

    StandardAssetVersionMode(String name) {
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
