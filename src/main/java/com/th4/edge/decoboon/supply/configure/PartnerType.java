package com.th4.edge.decoboon.supply.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 供应链合作方类型。 */
@Note("合作方类型")
public enum PartnerType implements Enumerable<String> {
    Supplier("材料供应商"),
    Factory("定制工厂"),
    LaborTeam("劳务班组"),
    Installer("安装班组"),
    Logistics("物流服务商"),
    Service("其他服务商");

    private final String name;

    PartnerType(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
