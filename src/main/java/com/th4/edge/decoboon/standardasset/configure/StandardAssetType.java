package com.th4.edge.decoboon.standardasset.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 企业运营标准资产类型。 */
@Note("企业运营标准资产类型")
public enum StandardAssetType implements Enumerable<String> {
    CatalogItem("报价项目库"),
    PackageTemplate("套餐模板"),
    PaymentPlanTemplate("付款计划模板"),
    ConstructionPlanTemplate("施工计划模板"),
    DocumentTemplate("单据/凭证模板");

    private final String name;

    StandardAssetType(String name) {
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
