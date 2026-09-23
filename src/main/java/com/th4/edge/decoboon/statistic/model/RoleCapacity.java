package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/**
 * 角色产能（设计方案 §10.4 决策：组织维度按「角色 + 门店/团队」，不新增部门对象）。
 *
 * <p>每个角色给出「人数 + 该角色的事业部口径绩效均值」，不虚构统一的产能公式。</p>
 */
@Note("角色产能")
public class RoleCapacity {

    @Note("角色")
    private String role;

    @Note("角色名称")
    private String label;

    @Note("在职人数")
    private Integer count = 0;

    @Note("绩效指标键")
    private String metricKey;

    @Note("绩效指标名称")
    private String metricLabel;

    @Note("绩效指标值（百分比）")
    private BigDecimal metricValue = BigDecimal.ZERO;

    public RoleCapacity() { }

    public RoleCapacity(String role, String label, Integer count,
                        String metricKey, String metricLabel, BigDecimal metricValue) {
        this.role = role;
        this.label = label;
        this.count = count;
        this.metricKey = metricKey;
        this.metricLabel = metricLabel;
        this.metricValue = metricValue;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public String getMetricLabel() { return metricLabel; }
    public void setMetricLabel(String metricLabel) { this.metricLabel = metricLabel; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
}
