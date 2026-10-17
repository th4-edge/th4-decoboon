package com.th4.edge.decoboon.blueprint.model;

import com.th4.edge.framework.core.annotation.Note;

/** 业务蓝图运行态：稳定标识、来源版本和规范化蓝图 JSON。 */
@Note("业务蓝图运行态")
public class BlueprintRuntime {

    @Note("蓝图标识")
    private String id;
    @Note("标题")
    private String title;
    @Note("版本")
    private String version;
    @Note("状态")
    private String status;
    @Note("最近核对提交")
    private String lastReviewedCommit;
    @Note("来源引用数量")
    private int sourceRefCount;
    @Note("当前查看角色")
    private String viewerRole;
    @Note("是否可见成本语义")
    private boolean costVisible;
    @Note("规范化蓝图 JSON")
    private String blueprintJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastReviewedCommit() { return lastReviewedCommit; }
    public void setLastReviewedCommit(String lastReviewedCommit) { this.lastReviewedCommit = lastReviewedCommit; }
    public int getSourceRefCount() { return sourceRefCount; }
    public void setSourceRefCount(int sourceRefCount) { this.sourceRefCount = sourceRefCount; }
    public String getViewerRole() { return viewerRole; }
    public void setViewerRole(String viewerRole) { this.viewerRole = viewerRole; }
    public boolean isCostVisible() { return costVisible; }
    public void setCostVisible(boolean costVisible) { this.costVisible = costVisible; }
    public String getBlueprintJson() { return blueprintJson; }
    public void setBlueprintJson(String blueprintJson) { this.blueprintJson = blueprintJson; }
}
