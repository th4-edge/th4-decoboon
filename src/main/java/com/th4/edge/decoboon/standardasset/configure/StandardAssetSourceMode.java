package com.th4.edge.decoboon.standardasset.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 标准资产来源模式。 */
@Note("标准资产来源模式")
public enum StandardAssetSourceMode implements Enumerable<String> {
    CompanyOnly("企业自建"),
    BaselineCopy("平台基线复制，不自动升级");

    private final String name;

    StandardAssetSourceMode(String name) {
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
