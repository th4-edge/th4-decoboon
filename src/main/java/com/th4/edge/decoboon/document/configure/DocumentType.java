package com.th4.edge.decoboon.document.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 平台单据类型。 */
@Note("单据类型")
public enum DocumentType implements Enumerable<String> {
    Quote("客户报价单"),
    Material("材料计划单"),
    Receipt("收款单"),
    Payment("付款单"),
    Contract("合同打印稿");

    private final String name;

    DocumentType(String name) { this.name = name; }
    @Override public String getName() { return name; }
}
