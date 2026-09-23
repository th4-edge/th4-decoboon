package com.th4.edge.decoboon.statistic.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 工作台栏位。
 *
 * <p>工作台只分三栏：需要处理、推进中、风险与待确认。栏位是<b>读模型投影</b>，
 * 不改变任何业务对象状态，也不参与写链路。</p>
 */
@Note("工作台栏位")
public enum WorkbenchBucket implements Enumerable<String> {
    NeedAction("需要处理"),
    InProgress("推进中"),
    Risk("风险与待确认");

    private final String name;

    WorkbenchBucket(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
