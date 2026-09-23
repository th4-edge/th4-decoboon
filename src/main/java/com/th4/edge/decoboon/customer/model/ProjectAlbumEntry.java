package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工地相册记录（业主可见）。
 *
 * <p>把「现场汇报」与「阶段验收」两路履约证据合并成一条按时间倒序的时间线，
 * 每条记录保留"谁 / 何时 / 哪个阶段 / 做了什么"四要素，供业主端以朋友圈方式回看。</p>
 */
@Note("工地相册记录")
public class ProjectAlbumEntry {

    @Note("记录键（source-businessId）")
    private String key;

    @Note("来源：Report 现场汇报 / Acceptance 阶段验收")
    private String source;

    @Note("来源业务主键（用于申请私有文件地址）")
    private Long businessId;

    @Note("类型值")
    private String type;

    @Note("类型名称")
    private String typeName;

    @Note("标题")
    private String title;

    @Note("正文")
    private String content;

    @Note("影像")
    private List<String> images = new ArrayList<>();

    @Note("提交人姓名")
    private String authorName;

    @Note("提交人角色名称")
    private String authorRole;

    @Note("所属施工阶段")
    private String stageName;

    @Note("状态名称")
    private String statusName;

    @Note("发生时间（汇报/验收提交时间）")
    private LocalDateTime occurredAt;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
}
