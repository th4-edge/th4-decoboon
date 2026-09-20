package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-竞品对比状态。 */
@Note("画像-竞品对比状态")
public enum PortraitCompetitor implements Enumerable<String> {

    None("未对比"),
    ComparingOneTwo("在对比1-2家"),
    ComparingThreePlus("在对比3家以上"),
    ChosenOther("已选定同行");

    private final String name;

    PortraitCompetitor(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
