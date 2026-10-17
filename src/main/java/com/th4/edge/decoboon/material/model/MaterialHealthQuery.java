package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

/** 项目材料业务体检查询。 */
@Note("项目材料业务体检查询")
public class MaterialHealthQuery {

    @Note("项目索引")
    private Long projectId;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
