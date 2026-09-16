package com.th4.edge.decoboon.quote.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价行的价格和成本组成类型。 */
@Note("报价组件类型")
public enum QuoteComponentType implements Enumerable<String> {
    Material("材料"),
    Labor("人工"),
    Auxiliary("辅料"),
    Machine("机械"),
    Freight("运输"),
    Installation("安装"),
    Production("生产"),
    Service("服务"),
    Tax("税费"),
    Other("其他");

    private final String name;

    QuoteComponentType(String name) {
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
