package com.th4.edge.decoboon.project.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.decoboon.common.model.BusinessFactNode;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 企业负责人「合同履约档案」聚合视图。
 *
 * <p>一次返回单份合同的经营结论：金额与回款、成本与利润、付款计划节点状态、
 * 工地进度、变更汇总与主材选材状态，供小程序合同履约档案页一屏展示。</p>
 *
 * <p>本模型只做读取聚合，不改变任何业务事实；金额、成本、利润口径分别来自
 * {@code ProjectContractContext}、{@code ProjectCostContext} 与真实应收/采购事实。</p>
 */
@Note("合同履约档案聚合")
public class ContractWorkbench {

    @Note("合同基线（含付款计划与施工计划快照）")
    private ProjectContract contract;

    @Note("回款结论")
    private Receivable receivable = new Receivable();

    @Note("付款计划节点与到账状态")
    private List<PaymentNode> paymentNodes = new ArrayList<>();

    @Note("成本与利润结论")
    private Cost cost = new Cost();

    @Note("工地进度结论")
    private Progress progress = new Progress();

    @Note("变更汇总")
    private Change change = new Change();

    @Note("主材选材状态")
    private Selection selection = new Selection();

    @Note("合同上游事实节点")
    private List<BusinessFactNode> upstreamFacts = new ArrayList<>();

    @Note("合同下游事实摘要")
    private List<BusinessFactNode> downstreamFacts = new ArrayList<>();

    @Note("上游来源缺口")
    private List<String> sourceGaps = new ArrayList<>();

    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public Receivable getReceivable() { return receivable; }
    public void setReceivable(Receivable receivable) { this.receivable = receivable; }
    public List<PaymentNode> getPaymentNodes() { return paymentNodes; }
    public void setPaymentNodes(List<PaymentNode> paymentNodes) { this.paymentNodes = paymentNodes; }
    public Cost getCost() { return cost; }
    public void setCost(Cost cost) { this.cost = cost; }
    public Progress getProgress() { return progress; }
    public void setProgress(Progress progress) { this.progress = progress; }
    public Change getChange() { return change; }
    public void setChange(Change change) { this.change = change; }
    public Selection getSelection() { return selection; }
    public void setSelection(Selection selection) { this.selection = selection; }
    public List<BusinessFactNode> getUpstreamFacts() { return upstreamFacts; }
    public void setUpstreamFacts(List<BusinessFactNode> upstreamFacts) { this.upstreamFacts = upstreamFacts; }
    public List<BusinessFactNode> getDownstreamFacts() { return downstreamFacts; }
    public void setDownstreamFacts(List<BusinessFactNode> downstreamFacts) { this.downstreamFacts = downstreamFacts; }
    public List<String> getSourceGaps() { return sourceGaps; }
    public void setSourceGaps(List<String> sourceGaps) { this.sourceGaps = sourceGaps; }

    /** 回款结论：应收、已收、未收与逾期，全部来自真实应收事实。 */
    @Note("回款结论")
    public static class Receivable {
        @Unit("元") @Note("应收总额") private BigDecimal receivableTotal = BigDecimal.ZERO;
        @Unit("元") @Note("已收金额") private BigDecimal receivedTotal = BigDecimal.ZERO;
        @Unit("元") @Note("未收金额") private BigDecimal outstandingTotal = BigDecimal.ZERO;
        @Unit("元") @Note("逾期未收金额") private BigDecimal overdueTotal = BigDecimal.ZERO;
        @Note("逾期笔数") private Integer overdueCount = 0;
        @Note("应收总笔数") private Integer totalCount = 0;

        public BigDecimal getReceivableTotal() { return receivableTotal; }
        public void setReceivableTotal(BigDecimal receivableTotal) { this.receivableTotal = receivableTotal; }
        public BigDecimal getReceivedTotal() { return receivedTotal; }
        public void setReceivedTotal(BigDecimal receivedTotal) { this.receivedTotal = receivedTotal; }
        public BigDecimal getOutstandingTotal() { return outstandingTotal; }
        public void setOutstandingTotal(BigDecimal outstandingTotal) { this.outstandingTotal = outstandingTotal; }
        public BigDecimal getOverdueTotal() { return overdueTotal; }
        public void setOverdueTotal(BigDecimal overdueTotal) { this.overdueTotal = overdueTotal; }
        public Integer getOverdueCount() { return overdueCount; }
        public void setOverdueCount(Integer overdueCount) { this.overdueCount = overdueCount; }
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    }

    /** 付款计划节点：计划比例与金额，叠加对应应收的真实到账状态。 */
    @Note("付款计划节点状态")
    public static class PaymentNode {
        @Note("节点索引") private Long nodeId;
        @Note("节点编码") private String code;
        @Note("节点名称") private String name;
        @Note("比例") private BigDecimal ratio;
        @Unit("元") @Note("节点金额") private BigDecimal amount;
        @Unit("元") @Note("已收金额") private BigDecimal receivedAmount = BigDecimal.ZERO;
        @Unit("元") @Note("未收金额") private BigDecimal outstandingAmount = BigDecimal.ZERO;
        @Note("触发方式") private String triggerType;
        @Note("触发阶段") private String triggerStageName;
        @Note("到期日") private LocalDate dueDate;
        @Note("收款状态") private String status;
        @Note("状态名称") private String statusName;
        @Note("关联应收索引") private Long receivableId;

        public Long getNodeId() { return nodeId; }
        public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getRatio() { return ratio; }
        public void setRatio(BigDecimal ratio) { this.ratio = ratio; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getReceivedAmount() { return receivedAmount; }
        public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
        public BigDecimal getOutstandingAmount() { return outstandingAmount; }
        public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public String getTriggerStageName() { return triggerStageName; }
        public void setTriggerStageName(String triggerStageName) { this.triggerStageName = triggerStageName; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
    }

    /** 成本与利润结论：成本不完整时毛利与毛利率必须为 null。 */
    @Note("成本与利润结论")
    public static class Cost {
        @Unit("元") @Note("预计成本（签约成本快照优先）") private BigDecimal estimatedCost = BigDecimal.ZERO;
        @Unit("元") @Note("实际成本（采购到货等实际事实优先）") private BigDecimal actualCost = BigDecimal.ZERO;
        @Note("成本是否完整") private Boolean costComplete = Boolean.FALSE;
        @Note("成本不完整项数") private Integer incompleteItemCount = 0;
        @Unit("元") @Note("当前动态毛利；成本不完整时为 null") private BigDecimal grossProfit;
        @Note("当前动态毛利率；成本不完整时为 null") private BigDecimal grossMargin;

        public BigDecimal getEstimatedCost() { return estimatedCost; }
        public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
        public BigDecimal getActualCost() { return actualCost; }
        public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
        public Boolean getCostComplete() { return costComplete; }
        public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
        public Integer getIncompleteItemCount() { return incompleteItemCount; }
        public void setIncompleteItemCount(Integer incompleteItemCount) { this.incompleteItemCount = incompleteItemCount; }
        public BigDecimal getGrossProfit() { return grossProfit; }
        public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
        public BigDecimal getGrossMargin() { return grossMargin; }
        public void setGrossMargin(BigDecimal grossMargin) { this.grossMargin = grossMargin; }
    }

    /** 工地进度结论：仅描述项目与阶段事实，不推断下一步。 */
    @Note("工地进度结论")
    public static class Progress {
        @Note("项目索引") private Long projectId;
        @Note("项目编号") private String projectNo;
        @Note("项目名称") private String projectName;
        @Note("项目状态") private String status;
        @Note("项目状态名称") private String statusName;
        @Note("整体进度（0-100）") private Integer percent = 0;
        @Note("当前阶段名称") private String currentStageName;
        @Note("阶段总数") private Integer stageTotal = 0;
        @Note("已完成阶段数") private Integer stageDone = 0;
        @Note("待验收数量") private Integer pendingAcceptanceCount = 0;
        @Note("合同基准开工") private LocalDate startDate;
        @Note("合同基准完工") private LocalDate planEndDate;
        @Note("预计完工（动态，排期驱动）") private LocalDate expectedEndDate;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getProjectNo() { return projectNo; }
        public void setProjectNo(String projectNo) { this.projectNo = projectNo; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public Integer getPercent() { return percent; }
        public void setPercent(Integer percent) { this.percent = percent; }
        public String getCurrentStageName() { return currentStageName; }
        public void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }
        public Integer getStageTotal() { return stageTotal; }
        public void setStageTotal(Integer stageTotal) { this.stageTotal = stageTotal; }
        public Integer getStageDone() { return stageDone; }
        public void setStageDone(Integer stageDone) { this.stageDone = stageDone; }
        public Integer getPendingAcceptanceCount() { return pendingAcceptanceCount; }
        public void setPendingAcceptanceCount(Integer pendingAcceptanceCount) { this.pendingAcceptanceCount = pendingAcceptanceCount; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getPlanEndDate() { return planEndDate; }
        public void setPlanEndDate(LocalDate planEndDate) { this.planEndDate = planEndDate; }
        public LocalDate getExpectedEndDate() { return expectedEndDate; }
        public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }
    }

    /** 变更汇总：只统计真实变更单状态。 */
    @Note("变更汇总")
    public static class Change {
        @Note("变更单总数") private Integer totalCount = 0;
        @Note("待审批数") private Integer pendingCount = 0;
        @Note("已审批数") private Integer approvedCount = 0;
        @Note("客户已确认数") private Integer confirmedCount = 0;
        @Note("已驳回或取消数") private Integer closedCount = 0;
        @Unit("元") @Note("累计变更金额（仅客户已确认）") private BigDecimal confirmedAmount = BigDecimal.ZERO;

        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        public Integer getPendingCount() { return pendingCount; }
        public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
        public Integer getApprovedCount() { return approvedCount; }
        public void setApprovedCount(Integer approvedCount) { this.approvedCount = approvedCount; }
        public Integer getConfirmedCount() { return confirmedCount; }
        public void setConfirmedCount(Integer confirmedCount) { this.confirmedCount = confirmedCount; }
        public Integer getClosedCount() { return closedCount; }
        public void setClosedCount(Integer closedCount) { this.closedCount = closedCount; }
        public BigDecimal getConfirmedAmount() { return confirmedAmount; }
        public void setConfirmedAmount(BigDecimal confirmedAmount) { this.confirmedAmount = confirmedAmount; }
    }

    /** 主材选材状态：只读投影，不改变报价、合同、成本与采购。 */
    @Note("主材选材状态")
    public static class Selection {
        @Note("最新选材记录索引") private Long selectionId;
        @Note("版本号") private Integer version;
        @Note("状态") private String status;
        @Note("状态名称") private String statusName;
        @Note("标题") private String title;
        @Note("正文摘要") private String summary;
        @Note("维护设计师") private String designerName;
        @Note("确认人姓名") private String confirmerName;
        @Note("确认时间") private LocalDateTime confirmedAt;
        @Note("选材记录总数") private Integer totalCount = 0;

        public Long getSelectionId() { return selectionId; }
        public void setSelectionId(Long selectionId) { this.selectionId = selectionId; }
        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getDesignerName() { return designerName; }
        public void setDesignerName(String designerName) { this.designerName = designerName; }
        public String getConfirmerName() { return confirmerName; }
        public void setConfirmerName(String confirmerName) { this.confirmerName = confirmerName; }
        public LocalDateTime getConfirmedAt() { return confirmedAt; }
        public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    }
}
