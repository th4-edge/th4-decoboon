package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 套餐基础行与设计分项的差异类型。 */
@Note("报价设计差异类型")
public enum QuoteDifferenceType implements Enumerable<String> {
    None("无差异"),
    Matched("匹配"),
    PackageOnly("套餐独有"),
    DesignOnly("设计新增"),
    QuantityChanged("数量变化"),
    Deduct("扣减"),
    Upgrade("升级"),
    Replace("替换"),
    Ignored("已忽略");

    private final String name;

    QuoteDifferenceType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
