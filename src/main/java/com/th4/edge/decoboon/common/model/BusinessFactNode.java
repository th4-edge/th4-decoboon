package com.th4.edge.decoboon.common.model;

import com.th4.edge.decoboon.common.configure.FactStage;
import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.decoboon.common.configure.FactVisibility;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 合同/项目事实链的统一节点契约。
 *
 * <p>节点是来源对象的索引和摘要，不复制完整明细，也不重算任何金额、进度或利润。</p>
 */
@Note("业务事实节点")
public class BusinessFactNode {

    @Note("节点 ID")
    private String nodeId;

    @Note("稳定节点键")
    private String nodeKey;

    @Note("事实阶段")
    private FactStage stage;

    @Note("事实类型")
    private FactType factType;

    @Note("对象类型")
    private String objectType;

    @Note("对象 ID")
    private Long objectId;

    @Note("所属项目 ID")
    private Long projectId;

    @Note("对象版本")
    private Integer version;

    @Note("是否当前有效事实")
    private Boolean current = Boolean.TRUE;

    @Note("状态")
    private String status;

    @Note("状态名称")
    private String statusName;

    @Note("业务发生时间")
    private LocalDateTime businessTime;

    @Note("系统记录时间")
    private LocalDateTime recordedAt;

    @Note("责任人")
    private String actorName;

    @Note("标题")
    private String title;

    @Note("摘要")
    private String summary;

    @Note("来源说明")
    private String sourceLabel;

    @Unit("元")
    @Note("权威金额摘要")
    private BigDecimal amount;

    @Note("数量摘要")
    private BigDecimal quantity;

    @Note("进度百分比")
    private Integer progress;

    @Note("成本是否完整")
    private Boolean costComplete;

    @Unit("%")
    @Note("毛利率摘要；成本不完整时为空")
    private BigDecimal grossMargin;

    @Unit("元")
    @Note("利润摘要；仅继承来源对象或结算快照的权威值")
    private BigDecimal profitAmount;

    @Note("缺失或不可确认说明")
    private String uncertainty;

    @Note("父节点 ID")
    private String parentNodeId;

    @Note("关联对象")
    private List<FactRelation> relations = new ArrayList<>();

    @Note("可见性")
    private FactVisibility visibility = FactVisibility.Internal;

    @Note("统一对象视图路径")
    private String navigationPath;

    @Note("返回路径")
    private String returnPath;

    @Note("返回锚点")
    private String returnAnchor;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getNodeKey() { return nodeKey; }
    public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }
    public FactStage getStage() { return stage; }
    public void setStage(FactStage stage) { this.stage = stage; }
    public FactType getFactType() { return factType; }
    public void setFactType(FactType factType) { this.factType = factType; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long objectId) { this.objectId = objectId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Boolean getCurrent() { return current; }
    public void setCurrent(Boolean current) { this.current = current; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public LocalDateTime getBusinessTime() { return businessTime; }
    public void setBusinessTime(LocalDateTime businessTime) { this.businessTime = businessTime; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public BigDecimal getGrossMargin() { return grossMargin; }
    public void setGrossMargin(BigDecimal grossMargin) { this.grossMargin = grossMargin; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public String getUncertainty() { return uncertainty; }
    public void setUncertainty(String uncertainty) { this.uncertainty = uncertainty; }
    public String getParentNodeId() { return parentNodeId; }
    public void setParentNodeId(String parentNodeId) { this.parentNodeId = parentNodeId; }
    public List<FactRelation> getRelations() { return relations; }
    public void setRelations(List<FactRelation> relations) { this.relations = relations; }
    public FactVisibility getVisibility() { return visibility; }
    public void setVisibility(FactVisibility visibility) { this.visibility = visibility; }
    public String getNavigationPath() { return navigationPath; }
    public void setNavigationPath(String navigationPath) { this.navigationPath = navigationPath; }
    public String getReturnPath() { return returnPath; }
    public void setReturnPath(String returnPath) { this.returnPath = returnPath; }
    public String getReturnAnchor() { return returnAnchor; }
    public void setReturnAnchor(String returnAnchor) { this.returnAnchor = returnAnchor; }

    /** 事件节点指向的来源对象关系。 */
    @Note("事实关联")
    public static class FactRelation {
        @Note("关系类型")
        private String relationType;
        @Note("对象类型")
        private String objectType;
        @Note("对象 ID")
        private Long objectId;
        @Note("对象版本")
        private Integer version;

        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getObjectType() { return objectType; }
        public void setObjectType(String objectType) { this.objectType = objectType; }
        public Long getObjectId() { return objectId; }
        public void setObjectId(Long objectId) { this.objectId = objectId; }
        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
    }
}
