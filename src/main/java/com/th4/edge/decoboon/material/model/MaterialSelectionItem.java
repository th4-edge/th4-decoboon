package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目主材选材分项读模型。
 *
 * <p>清单来源为合同中的 Material 行；未创建选材结果时状态为空，已创建时返回最新结果版本。
 * 下单状态从项目材料计划派生，不新增人工下单状态。</p>
 */
@Note("项目主材选材分项")
public class MaterialSelectionItem {

    @Note("合同主材行索引") private Long itemId;
    @Note("项目索引") private Long projectId;
    @Note("项目编号") private String projectNo;
    @Note("项目名称") private String projectName;
    @Note("房屋索引") private Long propertyId;
    @Note("房屋地址") private String propertyAddress;
    @Note("客户名称") private String customerName;
    @Note("责任设计师") private String designerName;
    @Note("合同索引") private Long contractId;
    @Note("合同编号") private String contractNo;
    @Note("空间名称") private String spaceName;
    @Note("材料分类") private String category;
    @Note("材料名称") private String materialName;
    @Note("合同品牌要求") private String brand;
    @Note("合同规格要求") private String spec;
    @Note("单位") private String unit;
    @Note("数量") private BigDecimal quantity;
    @Note("排序") private Integer sort;
    @Note("是否必选") private boolean required;
    @Note("最新选材记录索引") private Long selectionId;
    @Note("最新版本号") private Integer version;
    @Note("选材状态值") private String status;
    @Note("选材状态名称") private String statusName;
    @Note("可读选材文本") private String selectionText;
    @Note("选材 JSON 快照") private String selectionJson;
    @Note("客户确认人姓名") private String confirmerName;
    @Note("确认操作员工姓名") private String confirmedByName;
    @Note("客户确认时间") private LocalDateTime confirmedAt;
    @Note("最近更新时间（毫秒时间戳）") private Long updatedAt;
    @Note("下单状态值") private String orderStatus;
    @Note("下单状态名称") private String orderStatusName;
    @Note("下一步动作") private String nextAction;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectNo() { return projectNo; }
    public void setProjectNo(String projectNo) { this.projectNo = projectNo; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getDesignerName() { return designerName; }
    public void setDesignerName(String designerName) { this.designerName = designerName; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public Long getSelectionId() { return selectionId; }
    public void setSelectionId(Long selectionId) { this.selectionId = selectionId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getSelectionText() { return selectionText; }
    public void setSelectionText(String selectionText) { this.selectionText = selectionText; }
    public String getSelectionJson() { return selectionJson; }
    public void setSelectionJson(String selectionJson) { this.selectionJson = selectionJson; }
    public String getConfirmerName() { return confirmerName; }
    public void setConfirmerName(String confirmerName) { this.confirmerName = confirmerName; }
    public String getConfirmedByName() { return confirmedByName; }
    public void setConfirmedByName(String confirmedByName) { this.confirmedByName = confirmedByName; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getOrderStatusName() { return orderStatusName; }
    public void setOrderStatusName(String orderStatusName) { this.orderStatusName = orderStatusName; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
}
