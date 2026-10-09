package com.th4.edge.decoboon.content.model;

import com.th4.edge.framework.core.annotation.Note;
import java.util.ArrayList;
import java.util.List;

/** 内容经营看板。 */
@Note("内容经营看板")
public class ContentDashboardReport {
    public record Metric(String key, String label, long value, String subText) {}
    private List<Metric> metrics = new ArrayList<>();
    private List<LeadTouch> recentTouches = new ArrayList<>();
    private List<ContentTask> recentTasks = new ArrayList<>();

    public List<Metric> getMetrics() { return metrics; }
    public void setMetrics(List<Metric> metrics) { this.metrics = metrics; }
    public List<LeadTouch> getRecentTouches() { return recentTouches; }
    public void setRecentTouches(List<LeadTouch> recentTouches) { this.recentTouches = recentTouches; }
    public List<ContentTask> getRecentTasks() { return recentTasks; }
    public void setRecentTasks(List<ContentTask> recentTasks) { this.recentTasks = recentTasks; }
}
