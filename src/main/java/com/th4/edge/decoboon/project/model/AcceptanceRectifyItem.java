package com.th4.edge.decoboon.project.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 整改中阶段清单项。
 *
 * <p>依据用户 2026-09-21 裁决：整改对象为阶段级、复用同一阶段与验收单、循环不限次数、只做质量事实不碰账。
 * 清单只回答三件事：哪个阶段卡在整改、第几轮、超没超期。</p>
 */
@Note("整改中阶段清单项")
public class AcceptanceRectifyItem {

    @Note("验收单索引")
    private Long acceptanceId;
    @Note("项目索引")
    private Long projectId;
    @Note("项目编号")
    private String projectNo;
    @Note("项目名称")
    private String projectName;
    @Note("阶段索引")
    private Long stageId;
    @Note("阶段名称")
    private String stageName;
    @Note("当前轮次号（已提交的轮次数）")
    private Integer roundNo;
    @Note("本轮驳回结论")
    private String conclusion;
    @Note("问题清单")
    private List<String> issues;
    @Note("整改责任人")
    private String rectificationOwnerName;
    @Note("整改类型")
    private String rectificationType;
    @Note("整改类型名称")
    private String rectificationTypeName;
    @Note("整改问题图片数量")
    private Integer rectificationIssueImageCount;
    @Note("整改截止日期")
    private LocalDate rectificationDueDate;
    @Note("逾期天数（未逾期为 0）")
    private Integer overdueDays;
    @Note("驳回时间")
    private LocalDateTime confirmedAt;
    @Note("下一步动作")
    private String nextAction;

    public Long getAcceptanceId() {
        return acceptanceId;
    }

    public void setAcceptanceId(Long acceptanceId) {
        this.acceptanceId = acceptanceId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectNo() {
        return projectNo;
    }

    public void setProjectNo(String projectNo) {
        this.projectNo = projectNo;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public String getRectificationOwnerName() {
        return rectificationOwnerName;
    }

    public void setRectificationOwnerName(String rectificationOwnerName) {
        this.rectificationOwnerName = rectificationOwnerName;
    }

    public String getRectificationType() {
        return rectificationType;
    }

    public void setRectificationType(String rectificationType) {
        this.rectificationType = rectificationType;
    }

    public String getRectificationTypeName() {
        return rectificationTypeName;
    }

    public void setRectificationTypeName(String rectificationTypeName) {
        this.rectificationTypeName = rectificationTypeName;
    }

    public Integer getRectificationIssueImageCount() {
        return rectificationIssueImageCount;
    }

    public void setRectificationIssueImageCount(Integer rectificationIssueImageCount) {
        this.rectificationIssueImageCount = rectificationIssueImageCount;
    }

    public LocalDate getRectificationDueDate() {
        return rectificationDueDate;
    }

    public void setRectificationDueDate(LocalDate rectificationDueDate) {
        this.rectificationDueDate = rectificationDueDate;
    }

    public Integer getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(Integer overdueDays) {
        this.overdueDays = overdueDays;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }
}
