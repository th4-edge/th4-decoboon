package com.th4.edge.decoboon.supply.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 合作方状态。 */
@Note("合作方状态")
public enum PartnerStatus implements Enumerable<String> {
    Active("合作中"),
    Disabled("已停用");

    private final String name;
    PartnerStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
