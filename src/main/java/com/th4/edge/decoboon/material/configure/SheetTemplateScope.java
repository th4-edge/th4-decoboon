package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 主材下单单据模板范围。
 *
 * <p>Platform = 平台内置行业基线（只读、版本化）；Company = 企业复制基线后自行维护的模板。</p>
 */
@Note("主材下单模板范围")
public enum SheetTemplateScope implements Enumerable<String> {

    Platform("平台行业基线"),
    Company("企业自定义");

    private final String name;

    SheetTemplateScope(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
