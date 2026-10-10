package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 区域面积/周长口径来源。
 *
 * <p>BR-040 §4 / DEC-062（EV-213）：区域面积与周长是设计师实测量房后经 CAD 计算录入的实测事实，
 * 不是由开间/进深推算的派生量。只有 {@link #Measured} 才允许作为工程量基数；
 * {@link #Estimated} 是历史遗留的推算值，仅用于提示复核，不得进入工程量与报价金额。</p>
 */
@Note("区域面积口径来源")
public enum AreaSource implements Enumerable<String> {
    Measured("实测"),
    Estimated("估算");

    private final String name;

    AreaSource(String name) {
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
