package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/**
 * 主材选材跟进台列表项。
 *
 * <p>按当前员工数据范围聚合「已签约进入履约、但主材选材尚未客户确认」的项目，
 * 直接给出当前事实、责任设计师与下一步动作，供企业负责人、店长、设计师在现场或管理时跟进。</p>
 */
@Note("主材选材跟进项")
public class SelectionBoardItem {

    @Note("项目索引")
    private Long projectId;
    @Note("项目编号")
    private String projectNo;
    @Note("项目名称")
    private String projectName;
    @Note("项目状态值")
    private String projectStatus;
    @Note("项目状态名称")
    private String projectStatusName;

    @Note("房屋索引")
    private Long propertyId;
    @Note("房屋地址")
    private String propertyAddress;
    @Note("小区")
    private String community;
    @Note("房屋面积")
    private BigDecimal area;

    @Note("客户名称")
    private String customerName;
    @Note("选材责任人（设计师）")
    private String designerName;

    @Note("选材记录索引（未创建时为空）")
    private Long selectionId;
    @Note("选材状态值（未创建时为空）")
    private String selectionStatus;
    @Note("选材状态名称")
    private String selectionStatusName;
    @Note("主材分项总数")
    private Integer totalItems;
    @Note("已选材分项数")
    private Integer selectedItems;
    @Note("已交付财务分项数")
    private Integer deliveredItems;
    @Note("待选材分项数")
    private Integer notSelectedItems;
    @Note("已下单分项数")
    private Integer orderedItems;
    @Note("选材进度（0-100）")
    private Integer progress;
    @Note("选材版本号")
    private Integer version;
    @Note("最近更新时间（毫秒时间戳）")
    private Long updatedAt;
    @Note("下一步动作")
    private String nextAction;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectNo() {
        return projectNo;
    }

    public void setProjectNo(String projectNo) {
        this.projectNo = projectNo;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
    }

    public String getProjectStatusName() {
        return projectStatusName;
    }

    public void setProjectStatusName(String projectStatusName) {
        this.projectStatusName = projectStatusName;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public String getPropertyAddress() {
        return propertyAddress;
    }

    public void setPropertyAddress(String propertyAddress) {
        this.propertyAddress = propertyAddress;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDesignerName() {
        return designerName;
    }

    public void setDesignerName(String designerName) {
        this.designerName = designerName;
    }

    public Long getSelectionId() {
        return selectionId;
    }

    public void setSelectionId(Long selectionId) {
        this.selectionId = selectionId;
    }

    public String getSelectionStatus() {
        return selectionStatus;
    }

    public void setSelectionStatus(String selectionStatus) {
        this.selectionStatus = selectionStatus;
    }

    public String getSelectionStatusName() {
        return selectionStatusName;
    }

    public void setSelectionStatusName(String selectionStatusName) {
        this.selectionStatusName = selectionStatusName;
    }

    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public Integer getSelectedItems() { return selectedItems; }
    public void setSelectedItems(Integer selectedItems) { this.selectedItems = selectedItems; }
    public Integer getDeliveredItems() { return deliveredItems; }
    public void setDeliveredItems(Integer deliveredItems) { this.deliveredItems = deliveredItems; }
    public Integer getNotSelectedItems() { return notSelectedItems; }
    public void setNotSelectedItems(Integer notSelectedItems) { this.notSelectedItems = notSelectedItems; }
    public Integer getOrderedItems() { return orderedItems; }
    public void setOrderedItems(Integer orderedItems) { this.orderedItems = orderedItems; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }
}
