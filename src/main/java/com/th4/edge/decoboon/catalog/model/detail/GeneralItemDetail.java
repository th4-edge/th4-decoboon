package com.th4.edge.decoboon.catalog.model.detail;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 综合其他项目专项配置。 */
@Note("综合其他项目明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_general_detail")
public class GeneralItemDetail extends CatalogItemDetail {

    @Id
    @SnowflakeId
    private Long detailId;

    @Note("费用性质")
    @Column(length = 64)
    private String feeType;

    @Note("发票类型")
    @Column(length = 64)
    private String invoiceType;

    @Note("成本归属")
    @Column(length = 64)
    private String allocationMode;

    @Note("是否可折扣")
    @Column(nullable = false)
    private Boolean discountable = Boolean.TRUE;

    @Note("是否需要审批")
    @Column(nullable = false)
    private Boolean approvalRequired = Boolean.FALSE;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }
    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }
    public String getAllocationMode() { return allocationMode; }
    public void setAllocationMode(String allocationMode) { this.allocationMode = allocationMode; }
    public Boolean getDiscountable() { return discountable; }
    public void setDiscountable(Boolean discountable) { this.discountable = discountable; }
    public Boolean getApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(Boolean approvalRequired) { this.approvalRequired = approvalRequired; }
}
