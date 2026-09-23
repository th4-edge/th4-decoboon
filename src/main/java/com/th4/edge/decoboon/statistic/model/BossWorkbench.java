package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;
import java.util.List;

/** 企业负责人首页聚合结果。 */
@Note("企业负责人首页")
public class BossWorkbench {

    private LocalDate asOf;
    private LocalDate month;
    private List<WorkbenchIndicator> indicators = List.of();
    private List<WorkbenchProject> attentionProjects = List.of();
    private List<BusinessEvent> businessEvents = List.of();

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public List<WorkbenchIndicator> getIndicators() { return indicators; }
    public void setIndicators(List<WorkbenchIndicator> indicators) { this.indicators = indicators; }
    public List<WorkbenchProject> getAttentionProjects() { return attentionProjects; }
    public void setAttentionProjects(List<WorkbenchProject> attentionProjects) { this.attentionProjects = attentionProjects; }
    public List<BusinessEvent> getBusinessEvents() { return businessEvents; }
    public void setBusinessEvents(List<BusinessEvent> businessEvents) { this.businessEvents = businessEvents; }
}
