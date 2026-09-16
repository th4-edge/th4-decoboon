package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 任务影像节点。 */
@Note("任务影像节点")
public enum TaskNodeKey implements Enumerable<String> {
    Start("施工进行"),
    Conceal("隐蔽工程"),
    Finish("施工完成");

    private final String name;

    TaskNodeKey(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
