package com.th4.edge.decoboon.plan.model;

import com.th4.edge.decoboon.plan.configure.StageAcceptanceMode;
import com.th4.edge.framework.core.annotation.Note;

/** 平台施工计划阶段基线。 */
@Note("施工计划阶段基线")
public class ConstructionStageTemplate {
    private String code;
    private String name;
    private Integer defaultDays = 1;
    private StageAcceptanceMode acceptanceMode = StageAcceptanceMode.None;
    private Integer sort = 0;
    private String description;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDefaultDays() { return defaultDays; }
    public void setDefaultDays(Integer defaultDays) { this.defaultDays = defaultDays; }
    public StageAcceptanceMode getAcceptanceMode() { return acceptanceMode; }
    public void setAcceptanceMode(StageAcceptanceMode acceptanceMode) { this.acceptanceMode = acceptanceMode; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
