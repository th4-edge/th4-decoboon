package com.th4.edge.decoboon.project.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 业主视角预览打开结果。 */
@Note("业主视角预览结果")
public enum OwnerPreviewResult implements Enumerable<String> {
    Success("成功"),
    Forbidden("拒绝"),
    NotFound("项目不存在或无权访问"),
    Error("预览异常");

    private final String name;

    OwnerPreviewResult(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }
}
