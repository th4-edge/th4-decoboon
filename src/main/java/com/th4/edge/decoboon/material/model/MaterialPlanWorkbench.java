package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.ArrayList;
import java.util.List;

/** 材料计划工作台：按项目聚合的指标与待办明细。 */
@Note("材料计划工作台")
public class MaterialPlanWorkbench {

    @Note("项目索引") private Long projectId;
    @Note("项目编号") private String projectNo;
    @Note("项目名称") private String projectName;
    @Note("项目状态名称") private String projectStatusName;
    @Note("房屋地址") private String propertyAddress;
    @Note("客户名称") private String customerName;
    @Note("责任设计师") private String designerName;
    @Note("项目经理") private String managerName;
    @Note("材料计划总数") private int totalItems;
    @Note("待下单数") private int readyToOrder;
    @Note("部分下单数") private int partiallyOrdered;
    @Note("已下单待到货数") private int pendingArrival;
    @Note("已到场数") private int arrived;
    @Note("逾期未到场数") private int delayed;
    @Note("明细") private List<MaterialPlanWorkbenchItem> items = new ArrayList<>();

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
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public int getReadyToOrder() { return readyToOrder; }
    public void setReadyToOrder(int readyToOrder) { this.readyToOrder = readyToOrder; }
    public int getPartiallyOrdered() { return partiallyOrdered; }
    public void setPartiallyOrdered(int partiallyOrdered) { this.partiallyOrdered = partiallyOrdered; }
    public int getPendingArrival() { return pendingArrival; }
    public void setPendingArrival(int pendingArrival) { this.pendingArrival = pendingArrival; }
    public int getArrived() { return arrived; }
    public void setArrived(int arrived) { this.arrived = arrived; }
    public int getDelayed() { return delayed; }
    public void setDelayed(int delayed) { this.delayed = delayed; }
    public List<MaterialPlanWorkbenchItem> getItems() { return items; }
    public void setItems(List<MaterialPlanWorkbenchItem> items) { this.items = items; }
}
