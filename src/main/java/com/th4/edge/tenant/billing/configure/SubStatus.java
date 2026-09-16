package com.th4.edge.tenant.billing.configure;

import com.th4.edge.framework.core.Attribute;
import com.th4.edge.framework.core.annotation.Note;

/**
 * 企业订阅状态。
 */
@Note("订阅状态")
public enum SubStatus implements Attribute<String> {

    Trial("试用中"),
    Active("使用中"),
    Expired("已到期"),
    Frozen("已冻结"),
    Closed("已关闭");

    private final String name;

    SubStatus(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getName() {
        return name;
    }
}
