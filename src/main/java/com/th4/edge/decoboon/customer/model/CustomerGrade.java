package com.th4.edge.decoboon.customer.model;

import com.th4.edge.decoboon.customer.configure.CustomerGradeLevel;
import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户分级只读投影。
 *
 * <p>分级由服务端按冻结规则计算（A ≥ 75、B 50–74、C &lt; 50），不是独立业务对象，
 * 不落库、不接受前端写入。{@code breakdown} 必须可解释，供业务员理解评分来源。</p>
 */
@Note("客户分级")
public class CustomerGrade {

    @Note("分级等级")
    private CustomerGradeLevel level = CustomerGradeLevel.C;

    @Note("总分")
    private Integer score = 0;

    @Note("计算时间")
    private LocalDateTime computedAt;

    @Note("评分明细")
    private List<Item> breakdown = new ArrayList<>();

    public CustomerGrade() { }

    public CustomerGrade(CustomerGradeLevel level, Integer score, LocalDateTime computedAt, List<Item> breakdown) {
        this.level = level;
        this.score = score;
        this.computedAt = computedAt;
        this.breakdown = breakdown == null ? new ArrayList<>() : breakdown;
    }

    public CustomerGradeLevel getLevel() { return level; }
    public void setLevel(CustomerGradeLevel level) { this.level = level; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
    public List<Item> getBreakdown() { return breakdown; }
    public void setBreakdown(List<Item> breakdown) { this.breakdown = breakdown; }

    /** 单个评分维度明细。 */
    @Note("评分维度明细")
    public static class Item {

        @Note("维度标识")
        private String dimension;

        @Note("维度名称")
        private String label;

        @Note("维度权重")
        private Integer weight = 0;

        @Note("维度得分")
        private Integer score = 0;

        @Note("得分原因")
        private String reason;

        public Item() { }

        public Item(String dimension, String label, Integer weight, Integer score, String reason) {
            this.dimension = dimension;
            this.label = label;
            this.weight = weight;
            this.score = score;
            this.reason = reason;
        }

        public String getDimension() { return dimension; }
        public void setDimension(String dimension) { this.dimension = dimension; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getWeight() { return weight; }
        public void setWeight(Integer weight) { this.weight = weight; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
