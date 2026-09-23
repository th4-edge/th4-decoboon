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

    /**
     * 稳定枚举值，供 {name, value, key} 枚举响应和入参解析使用。
     *
     * <p>历史缺陷：本枚举曾未实现该方法，`Enumerable#getValue()` 默认返回 null，
     * 导致 `project/task-node/save` 的入参解析抛 NPE（响应 `undefinedErrorCode`），
     * 影像节点留证整体不可用。</p>
     */
    @Override
    public String getValue() {
        return name();
    }
}
