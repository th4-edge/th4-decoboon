package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 企业负责人增减项工作台查询。 */
@Note("企业负责人增减项工作台查询")
public class BossContractChangeQuery {

    private String view;
    private String keyword;
    private Long projectId;
    private Long ownerId;
    private String dimension;

    public String getView() { return view; }
    public void setView(String view) { this.view = view; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
}
