package com.th4.edge.decoboon.catalog.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价项目来源类型；P0 仅使用手工维护与批量导入。 */
@Note("报价项目来源")
public enum CatalogSourceType implements Enumerable<String> {

    Manual("手工维护"),
    Import("批量导入"),
    Migration("服务迁移");

    private final String name;

    CatalogSourceType(String name) {
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
