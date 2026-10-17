package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 项目材料业务体检：真实材料事实、成本完整性、当前阶段与下一步。 */
@Note("项目材料业务体检")
public class MaterialHealthReport {

    @Note("项目索引")
    private Long projectId;
    @Note("项目编号")
    private String projectNo;
    @Note("项目名称")
    private String projectName;
    @Note("项目状态名称")
    private String projectStatusName;
    @Note("房屋地址")
    private String propertyAddress;
    @Note("客户名称")
    private String customerName;
    @Note("责任设计师")
    private String designerName;
    @Note("项目经理")
    private String managerName;
    @Note("是否有材料计划")
    private boolean hasMaterialPlan;
    @Note("材料计划总数")
    private int totalItems;
    @Note("已选材数")
    private int selectedItems;
    @Note("未选材数")
    private int unselectedItems;
    @Note("待下单数")
    private int readyToOrder;
    @Note("部分下单数")
    private int partiallyOrdered;
    @Note("已下单待到货数")
    private int pendingArrival;
    @Note("部分到场数")
    private int partiallyArrived;
    @Note("已到场数")
    private int arrived;
    @Note("逾期未到场数")
    private int delayed;
    @Note("有效采购单数")
    private int purchaseCount;
    @Note("未定价采购单数")
    private int unpricedPurchaseCount;
    @Note("缺少供应商采购单数")
    private int missingSupplierCount;
    @Note("成本是否完整")
    private boolean costComplete;
    @Note("成本不完整项数")
    private int costIncompleteCount;
    @Note("采购应付金额")
    private BigDecimal materialCost = BigDecimal.ZERO;
    @Note("预计成本")
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    @Note("实际成本")
    private BigDecimal actualCost = BigDecimal.ZERO;
    @Note("利润；成本不完整时为 null")
    private BigDecimal profitAmount;
    @Note("成本状态说明")
    private String costLabel;
    @Note("当前阶段键")
    private String stageKey;
    @Note("当前阶段名称")
    private String stageLabel;
    @Note("当前阶段摘要")
    private String stageSummary;
    @Note("下一步")
    private String nextAction;
    @Note("项目结算状态")
    private String settlementStatus;
    @Note("校核项")
    private List<MaterialHealthIssue> issues = new ArrayList<>();
    @Note("聚焦明细")
    private List<MaterialHealthRow> rows = new ArrayList<>();

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectNo() { return projectNo; }
    public void setProjectNo(String projectNo) { this.projectNo = projectNo; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectStatusName() { return projectStatusName; }
    public void setProjectStatusName(String projectStatusName) { this.projectStatusName = projectStatusName; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public boolean isHasMaterialPlan() { return hasMaterialPlan; }
    public void setHasMaterialPlan(boolean hasMaterialPlan) { this.hasMaterialPlan = hasMaterialPlan; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public int getSelectedItems() { return selectedItems; }
    public void setSelectedItems(int selectedItems) { this.selectedItems = selectedItems; }
    public int getUnselectedItems() { return unselectedItems; }
    public void setUnselectedItems(int unselectedItems) { this.unselectedItems = unselectedItems; }
    public int getReadyToOrder() { return readyToOrder; }
    public void setReadyToOrder(int readyToOrder) { this.readyToOrder = readyToOrder; }
    public int getPartiallyOrdered() { return partiallyOrdered; }
    public void setPartiallyOrdered(int partiallyOrdered) { this.partiallyOrdered = partiallyOrdered; }
    public int getPendingArrival() { return pendingArrival; }
    public void setPendingArrival(int pendingArrival) { this.pendingArrival = pendingArrival; }
    public int getPartiallyArrived() { return partiallyArrived; }
    public void setPartiallyArrived(int partiallyArrived) { this.partiallyArrived = partiallyArrived; }
    public int getArrived() { return arrived; }
    public void setArrived(int arrived) { this.arrived = arrived; }
    public int getDelayed() { return delayed; }
    public void setDelayed(int delayed) { this.delayed = delayed; }
    public int getPurchaseCount() { return purchaseCount; }
    public void setPurchaseCount(int purchaseCount) { this.purchaseCount = purchaseCount; }
    public int getUnpricedPurchaseCount() { return unpricedPurchaseCount; }
    public void setUnpricedPurchaseCount(int unpricedPurchaseCount) { this.unpricedPurchaseCount = unpricedPurchaseCount; }
    public int getMissingSupplierCount() { return missingSupplierCount; }
    public void setMissingSupplierCount(int missingSupplierCount) { this.missingSupplierCount = missingSupplierCount; }
    public boolean isCostComplete() { return costComplete; }
    public void setCostComplete(boolean costComplete) { this.costComplete = costComplete; }
    public int getCostIncompleteCount() { return costIncompleteCount; }
    public void setCostIncompleteCount(int costIncompleteCount) { this.costIncompleteCount = costIncompleteCount; }
    public BigDecimal getMaterialCost() { return materialCost; }
    public void setMaterialCost(BigDecimal materialCost) { this.materialCost = materialCost; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public BigDecimal getProfitAmount() { return profitAmount; }
    public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
    public String getCostLabel() { return costLabel; }
    public void setCostLabel(String costLabel) { this.costLabel = costLabel; }
    public String getStageKey() { return stageKey; }
    public void setStageKey(String stageKey) { this.stageKey = stageKey; }
    public String getStageLabel() { return stageLabel; }
    public void setStageLabel(String stageLabel) { this.stageLabel = stageLabel; }
    public String getStageSummary() { return stageSummary; }
    public void setStageSummary(String stageSummary) { this.stageSummary = stageSummary; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(String settlementStatus) { this.settlementStatus = settlementStatus; }
    public List<MaterialHealthIssue> getIssues() { return issues; }
    public void setIssues(List<MaterialHealthIssue> issues) { this.issues = issues; }
    public List<MaterialHealthRow> getRows() { return rows; }
    public void setRows(List<MaterialHealthRow> rows) { this.rows = rows; }
}
