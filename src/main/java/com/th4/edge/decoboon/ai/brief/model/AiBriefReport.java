package com.th4.edge.decoboon.ai.brief.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** AI 经营简报只读结果。 */
@Note("AI经营简报")
public class AiBriefReport {

    @Note("统计月份")
    private LocalDate month;

    @Note("数据截止时间")
    private LocalDateTime asOf;

    @Note("一句话经营现状")
    private String summary;

    @Note("业务事实")
    private List<AiBriefFact> facts = new ArrayList<>();

    @Note("行动建议")
    private List<AiBriefAction> actions = new ArrayList<>();

    @Note("成本状态")
    private AiBriefCostStatus costStatus = new AiBriefCostStatus();

    @Note("本次生成用量")
    private AiBriefUsage usage = new AiBriefUsage();

    @Note("生成器版本")
    private String generatorVersion;

    @Note("来源记录")
    private List<AiBriefSource> sources = new ArrayList<>();

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<AiBriefFact> getFacts() { return facts; }
    public void setFacts(List<AiBriefFact> facts) { this.facts = facts; }
    public List<AiBriefAction> getActions() { return actions; }
    public void setActions(List<AiBriefAction> actions) { this.actions = actions; }
    public AiBriefCostStatus getCostStatus() { return costStatus; }
    public void setCostStatus(AiBriefCostStatus costStatus) { this.costStatus = costStatus; }
    public AiBriefUsage getUsage() { return usage; }
    public void setUsage(AiBriefUsage usage) { this.usage = usage; }
    public String getGeneratorVersion() { return generatorVersion; }
    public void setGeneratorVersion(String generatorVersion) { this.generatorVersion = generatorVersion; }
    public List<AiBriefSource> getSources() { return sources; }
    public void setSources(List<AiBriefSource> sources) { this.sources = sources; }

    /** 对象级来源。 */
    @Note("AI经营简报来源")
    public static class AiBriefSource {
        private String provider;
        private String objectType;
        private Long objectId;
        private String version;
        private LocalDateTime updatedAt;

        public AiBriefSource() { }

        public AiBriefSource(String provider, String objectType, Long objectId,
                             String version, LocalDateTime updatedAt) {
            this.provider = provider;
            this.objectType = objectType;
            this.objectId = objectId;
            this.version = version;
            this.updatedAt = updatedAt;
        }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getObjectType() { return objectType; }
        public void setObjectType(String objectType) { this.objectType = objectType; }
        public Long getObjectId() { return objectId; }
        public void setObjectId(Long objectId) { this.objectId = objectId; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    /** 经营事实。 */
    @Note("AI经营事实")
    public static class AiBriefFact {
        private String category;
        private String label;
        private String value;
        private String note;
        private String tone;

        public AiBriefFact() { }

        public AiBriefFact(String category, String label, String value, String note, String tone) {
            this.category = category;
            this.label = label;
            this.value = value;
            this.note = note;
            this.tone = tone;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getTone() { return tone; }
        public void setTone(String tone) { this.tone = tone; }
    }

    /** 行动建议。 */
    @Note("AI经营行动建议")
    public static class AiBriefAction {
        private String level;
        private String title;
        private String reason;
        private String ownerName;
        private String dueText;
        private String targetRoute;
        private String businessType;
        private Long businessId;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public String getDueText() { return dueText; }
        public void setDueText(String dueText) { this.dueText = dueText; }
        public String getTargetRoute() { return targetRoute; }
        public void setTargetRoute(String targetRoute) { this.targetRoute = targetRoute; }
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
        public Long getBusinessId() { return businessId; }
        public void setBusinessId(Long businessId) { this.businessId = businessId; }
    }

    /** 成本完整性。 */
    @Note("AI经营成本状态")
    public static class AiBriefCostStatus {
        private Boolean complete = Boolean.TRUE;
        private Integer incompleteProjectCount = 0;
        private String message;

        public Boolean getComplete() { return complete; }
        public void setComplete(Boolean complete) { this.complete = complete; }
        public Integer getIncompleteProjectCount() { return incompleteProjectCount; }
        public void setIncompleteProjectCount(Integer incompleteProjectCount) { this.incompleteProjectCount = incompleteProjectCount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /** 本次生成用量。 */
    @Note("AI经营简报用量")
    public static class AiBriefUsage {
        private Long callId;
        @Unit("单位")
        private Long resourceUnits;
        @Unit("点")
        private Long shadowBillableUnits;
        private String rateCardVersion;

        public Long getCallId() { return callId; }
        public void setCallId(Long callId) { this.callId = callId; }
        public Long getResourceUnits() { return resourceUnits; }
        public void setResourceUnits(Long resourceUnits) { this.resourceUnits = resourceUnits; }
        public Long getShadowBillableUnits() { return shadowBillableUnits; }
        public void setShadowBillableUnits(Long shadowBillableUnits) { this.shadowBillableUnits = shadowBillableUnits; }
        public String getRateCardVersion() { return rateCardVersion; }
        public void setRateCardVersion(String rateCardVersion) { this.rateCardVersion = rateCardVersion; }
    }
}
