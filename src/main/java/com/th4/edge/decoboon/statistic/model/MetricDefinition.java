package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 对外指标的完整口径说明（口径身份证）。 */
@Note("指标口径说明")
public class MetricDefinition {

    private String key;
    private String name;
    private String userLabel;
    private String category;
    private String definition;
    private String formula;
    private String source;
    private String timeScope;
    private String dataScope;
    private String includes;
    private String excludes;
    private String missingValue;
    private String example;
    private String updatedAt;
    private String status;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUserLabel() { return userLabel; }
    public void setUserLabel(String userLabel) { this.userLabel = userLabel; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTimeScope() { return timeScope; }
    public void setTimeScope(String timeScope) { this.timeScope = timeScope; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public String getIncludes() { return includes; }
    public void setIncludes(String includes) { this.includes = includes; }
    public String getExcludes() { return excludes; }
    public void setExcludes(String excludes) { this.excludes = excludes; }
    public String getMissingValue() { return missingValue; }
    public void setMissingValue(String missingValue) { this.missingValue = missingValue; }
    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
