package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.statistic.configure.GuideStatus;
import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;
import java.util.List;

/** 当前角色启动引导状态投影。 */
@Note("全角色启动引导")
public class OnboardingGuide {

    private String audience;
    private String audienceLabel;
    private String title;
    private String subtitle;
    private GuideStatus status = GuideStatus.Unknown;
    private int requiredTotal;
    private int requiredCompleted;
    private String nextAction;
    private String message;
    private List<GuideStep> steps = List.of();
    private LocalDateTime refreshedAt;

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public String getAudienceLabel() { return audienceLabel; }
    public void setAudienceLabel(String audienceLabel) { this.audienceLabel = audienceLabel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public GuideStatus getStatus() { return status; }
    public void setStatus(GuideStatus status) { this.status = status; }
    public int getRequiredTotal() { return requiredTotal; }
    public void setRequiredTotal(int requiredTotal) { this.requiredTotal = requiredTotal; }
    public int getRequiredCompleted() { return requiredCompleted; }
    public void setRequiredCompleted(int requiredCompleted) { this.requiredCompleted = requiredCompleted; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<GuideStep> getSteps() { return steps; }
    public void setSteps(List<GuideStep> steps) { this.steps = steps; }
    public LocalDateTime getRefreshedAt() { return refreshedAt; }
    public void setRefreshedAt(LocalDateTime refreshedAt) { this.refreshedAt = refreshedAt; }
}
