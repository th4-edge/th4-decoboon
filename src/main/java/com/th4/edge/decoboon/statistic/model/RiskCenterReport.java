package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 风险中心报表（企业负责人视角，读侧）。
 *
 * <p>数据来源为既有 {@code BusinessAlertContext} 的 4 类经营预警，不新增第二套风险规则；
 * 处置动作文案统一取 {@code BusinessAlertType#nextAction()}，与首页、项目台账同源。</p>
 *
 * <p>当前边界：仅覆盖逾期应收 / 停工与交付风险 / 材料超领 / 合同变更率异常 4 类；
 * 客户投诉、合规到期、毛利跌破阈值需先建业务对象或扩展 {@code BusinessAlertType}；
 * 处置状态与指派需要 {@code RiskItem} 实体与处置日志，属 P2.1。</p>
 */
@Note("风险中心报表")
public class RiskCenterReport {

    @Note("统计截止日")
    private LocalDate asOf;

    @Note("待处理风险总数")
    private Integer totalCount = 0;

    @Note("高风险（严重）数量")
    private Integer highCount = 0;

    @Note("中风险（警告）数量")
    private Integer mediumCount = 0;

    @Note("低风险（关注）数量")
    private Integer lowCount = 0;

    @Note("风险类型分布")
    private List<RiskTypeDistribution> typeDistribution = List.of();

    @Note("责任人分布（按预警归属人聚合）")
    private List<RiskResponsibility> responsibilityDistribution = List.of();

    @Note("待处理风险清单")
    private List<RiskItemView> items = List.of();

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getHighCount() { return highCount; }
    public void setHighCount(Integer highCount) { this.highCount = highCount; }
    public Integer getMediumCount() { return mediumCount; }
    public void setMediumCount(Integer mediumCount) { this.mediumCount = mediumCount; }
    public Integer getLowCount() { return lowCount; }
    public void setLowCount(Integer lowCount) { this.lowCount = lowCount; }
    public List<RiskTypeDistribution> getTypeDistribution() { return typeDistribution; }
    public void setTypeDistribution(List<RiskTypeDistribution> typeDistribution) { this.typeDistribution = typeDistribution; }
    public List<RiskResponsibility> getResponsibilityDistribution() { return responsibilityDistribution; }
    public void setResponsibilityDistribution(List<RiskResponsibility> responsibilityDistribution) { this.responsibilityDistribution = responsibilityDistribution; }
    public List<RiskItemView> getItems() { return items; }
    public void setItems(List<RiskItemView> items) { this.items = items; }

    /** 风险类型分布。 */
    @Note("风险类型分布")
    public static class RiskTypeDistribution {
        private String key;
        private String label;
        private String description;
        private Integer count = 0;
        private Integer highCount = 0;

        public RiskTypeDistribution() { }

        public RiskTypeDistribution(String key, String label, String description, Integer count, Integer highCount) {
            this.key = key;
            this.label = label;
            this.description = description;
            this.count = count;
            this.highCount = highCount;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Integer getHighCount() { return highCount; }
        public void setHighCount(Integer highCount) { this.highCount = highCount; }
    }

    /** 责任人分布。 */
    @Note("责任人分布")
    public static class RiskResponsibility {
        private String name;
        private Integer count = 0;
        private Integer highCount = 0;

        public RiskResponsibility() { }

        public RiskResponsibility(String name, Integer count, Integer highCount) {
            this.name = name;
            this.count = count;
            this.highCount = highCount;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Integer getHighCount() { return highCount; }
        public void setHighCount(Integer highCount) { this.highCount = highCount; }
    }

    /** 风险清单行。 */
    @Note("风险清单行")
    public static class RiskItemView {
        private String key;
        private String type;
        private String typeLabel;
        private String level;
        private String levelLabel;
        private String title;
        private String message;
        private String projectName;
        private String customerName;
        private String ownerName;
        @Unit("元")
        private BigDecimal amount = BigDecimal.ZERO;
        private LocalDate dueDate;
        @Note("时效文案：逾期 N 天 / 剩余 N 天 / 未设到期日")
        private String timeText;
        @Note("时效色调：danger / warn / normal")
        private String timeTone = "normal";
        @Note("建议处置动作（BusinessAlertType#nextAction）")
        private String nextAction;
        @Note("处置单主键；为空表示同步异常，前端应禁用动作")
        private Long riskId;
        @Note("处置状态：Pending / Handling / Closed / Void")
        private String status;
        @Note("处置状态名称")
        private String statusLabel;
        @Note("当前处置责任人（可能已与预警默认责任人不同）")
        private String disposerName;
        @Note("应处置截止时间")
        private java.time.LocalDateTime dueAt;
        @Note("时效提示：超时 N 小时 / 剩余 N 小时 / 今日到期")
        private String dueText;
        @Note("时效色调：danger / warn / normal")
        private String dueTone = "normal";
        @Note("重开轮次")
        private Integer roundNo = 1;
        @Note("同级复现次数")
        private Integer recurrenceCount = 0;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTypeLabel() { return typeLabel; }
        public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getLevelLabel() { return levelLabel; }
        public void setLevelLabel(String levelLabel) { this.levelLabel = levelLabel; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public String getTimeText() { return timeText; }
        public void setTimeText(String timeText) { this.timeText = timeText; }
        public String getTimeTone() { return timeTone; }
        public void setTimeTone(String timeTone) { this.timeTone = timeTone; }
        public String getNextAction() { return nextAction; }
        public void setNextAction(String nextAction) { this.nextAction = nextAction; }
        public Integer getRecurrenceCount() { return recurrenceCount; }
        public void setRecurrenceCount(Integer recurrenceCount) { this.recurrenceCount = recurrenceCount; }
        public Integer getRoundNo() { return roundNo; }
        public void setRoundNo(Integer roundNo) { this.roundNo = roundNo; }
        public String getDueTone() { return dueTone; }
        public void setDueTone(String dueTone) { this.dueTone = dueTone; }
        public String getDueText() { return dueText; }
        public void setDueText(String dueText) { this.dueText = dueText; }
        public java.time.LocalDateTime getDueAt() { return dueAt; }
        public void setDueAt(java.time.LocalDateTime dueAt) { this.dueAt = dueAt; }
        public String getDisposerName() { return disposerName; }
        public void setDisposerName(String disposerName) { this.disposerName = disposerName; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getRiskId() { return riskId; }
        public void setRiskId(Long riskId) { this.riskId = riskId; }
    }
}
