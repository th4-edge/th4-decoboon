package com.th4.edge.decoboon.project.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;
import java.util.List;

/** 企业负责人项目经营聚合结果。 */
@Note("企业负责人项目经营聚合")
public class ProjectOverviewReport {

    private LocalDate asOf;
    private LocalDate month;
    private List<ProjectMetricCard> metrics = List.of();
    private List<ProjectStatusCount> statusCounts = List.of();
    private List<ProjectStatusCount> contractCounts = List.of();
    private List<Project> projects = List.of();
    private Integer current = 1;
    private Integer size = 20;
    private Long total = 0L;
    private Boolean hasMore = Boolean.FALSE;

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public List<ProjectMetricCard> getMetrics() { return metrics; }
    public void setMetrics(List<ProjectMetricCard> metrics) {
        this.metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
    public List<ProjectStatusCount> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(List<ProjectStatusCount> statusCounts) {
        this.statusCounts = statusCounts == null ? List.of() : List.copyOf(statusCounts);
    }
    public List<ProjectStatusCount> getContractCounts() { return contractCounts; }
    public void setContractCounts(List<ProjectStatusCount> contractCounts) {
        this.contractCounts = contractCounts == null ? List.of() : List.copyOf(contractCounts);
    }
    public List<Project> getProjects() { return projects; }
    public void setProjects(List<Project> projects) {
        this.projects = projects == null ? List.of() : List.copyOf(projects);
    }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Boolean getHasMore() { return hasMore; }
    public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }
}
