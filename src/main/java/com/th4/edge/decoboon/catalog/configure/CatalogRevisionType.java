package com.th4.edge.decoboon.catalog.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价项目修订类型。 */
@Note("报价项目修订类型")
public enum CatalogRevisionType implements Enumerable<String> {
    Create("新增"),
    Update("更新"),
    Disable("停用"),
    Enable("启用"),
    Delete("删除");

    private final String name;

    CatalogRevisionType(String name) { this.name = name; }

    @Override
    public String getName() { return name; }

    @Override
    public String getValue() { return toString(); }
}
