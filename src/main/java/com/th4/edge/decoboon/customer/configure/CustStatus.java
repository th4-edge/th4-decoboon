package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 客户生命周期状态。
 */
@Note("客户状态")
public enum CustStatus implements Enumerable<String> {
    New("新客户"),
    Following("跟进中"),
    PendingMeasure("待量房"),
    Quoted("已报价"),
    Signed("已签约"),
    Serving("履约中"),
    Finished("已完工"),
    Lost("已流失"),
    Dormant("休眠客户");

    private final String name;

    CustStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
