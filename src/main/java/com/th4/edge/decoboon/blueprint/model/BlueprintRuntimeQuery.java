package com.th4.edge.decoboon.blueprint.model;

import com.th4.edge.framework.core.annotation.Note;

/** 业务蓝图运行态查询。 */
@Note("业务蓝图运行态查询")
public class BlueprintRuntimeQuery {

    @Note("蓝图标识")
    private String blueprintId;

    public String getBlueprintId() { return blueprintId; }
    public void setBlueprintId(String blueprintId) { this.blueprintId = blueprintId; }
}
