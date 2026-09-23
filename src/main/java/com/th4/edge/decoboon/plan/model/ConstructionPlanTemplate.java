package com.th4.edge.decoboon.plan.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.decoboon.plan.configure.PlanTemplateSource;
import com.th4.edge.decoboon.plan.configure.PlanTemplateStatus;

import java.util.List;

/** 平台施工计划模板基线。 */
@Note("施工计划模板基线")
public class ConstructionPlanTemplate {
    private Long templateId;
    private String code;
    private Integer version = 1;
    private String name;
    private String description;
    private Integer totalDays = 0;
    private Integer sort = 0;
    private PlanTemplateSource source = PlanTemplateSource.Baseline;
    private PlanTemplateStatus status = PlanTemplateStatus.Enabled;
    private Boolean defaultPlan = Boolean.FALSE;
    private List<ConstructionStageTemplate> stages = List.of();

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public PlanTemplateSource getSource() { return source; }
    public void setSource(PlanTemplateSource source) { this.source = source; }
    public PlanTemplateStatus getStatus() { return status; }
    public void setStatus(PlanTemplateStatus status) { this.status = status; }
    public Boolean getDefaultPlan() { return defaultPlan; }
    public void setDefaultPlan(Boolean defaultPlan) { this.defaultPlan = defaultPlan; }
    public List<ConstructionStageTemplate> getStages() { return stages; }
    public void setStages(List<ConstructionStageTemplate> stages) { this.stages = stages == null ? List.of() : List.copyOf(stages); }
}
