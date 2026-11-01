package com.th4.edge.decoboon.supply.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.finance.configure.SettlementMode;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 供应商、工厂、劳务班组等合作方。 */
@Note("供应链合作方")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_partner", indexes = {
        @Index(name = "idx_partner_company", columnList = "com_id, partner_id"),
        @Index(name = "idx_partner_company_type", columnList = "com_id, type, partner_id")
})
public class Partner extends AuditModel {

    @Id @SnowflakeId @Note("合作方索引") private Long partnerId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @Enumerated(EnumType.STRING) @Column(length = 24, nullable = false) @Note("合作方类型") private PartnerType type = PartnerType.Supplier;
    @Keyword @Column(length = 128, nullable = false) @Note("合作方名称") private String name;
    @Keyword @Column(length = 32) @Note("联系人") private String contactName;
    @Keyword @Column(length = 20) @Note("联系电话") private String telNo;
    @Column(length = 64) @Note("税号") private String taxNo;
    @Column(length = 256) @Note("地址") private String addr;
    @Column(length = 64) @Note("结算方式") private String settlementTerms;

    @Note("账期模式：收货日加账期天数 / 对账月结日")
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_mode", length = 16)
    private SettlementMode settlementMode;

    @Note("账期天数")
    @Column(name = "settlement_days")
    private Integer settlementDays;

    @Note("对账截账日（每月几号）")
    @Column(name = "cutoff_day")
    private Integer cutoffDay;
    @Column(length = 64) @Note("开户行") private String bankName;
    @Column(length = 64) @Note("银行账号") private String bankAccount;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("状态") private PartnerStatus status = PartnerStatus.Active;
    @Column(length = 500) @Note("备注") private String remark;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "db_partner_brand", joinColumns = @JoinColumn(name = "partner_id"))
    @Column(name = "brand", length = 128, nullable = false)
    @Note("可供应品牌")
    private Set<String> brands = new LinkedHashSet<>();

    /** 付款前核对供应商收款主体的付款码图片，仅保存私有存储对象键。 */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "db_partner_payment_code_image", joinColumns = @JoinColumn(name = "partner_id"))
    @OrderColumn(name = "sort_no")
    @Column(name = "url", length = 512, nullable = false)
    @Note("供应商付款码图片")
    private List<String> paymentCodeImages = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "creator_id", nullable = false) @Note("创建人") private Employee createdBy;
    @Transient @Note("应付总额") private java.math.BigDecimal payableAmount;
    @Transient @Note("已付总额") private java.math.BigDecimal paidAmount;
    @Transient @Note("未付余额") private java.math.BigDecimal outstandingAmount;

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public PartnerType getType() { return type; }
    public void setType(PartnerType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public String getTaxNo() { return taxNo; }
    public void setTaxNo(String taxNo) { this.taxNo = taxNo; }
    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }
    public SettlementMode getSettlementMode() { return settlementMode; }
    public void setSettlementMode(SettlementMode settlementMode) { this.settlementMode = settlementMode; }
    public Integer getSettlementDays() { return settlementDays; }
    public void setSettlementDays(Integer settlementDays) { this.settlementDays = settlementDays; }
    public Integer getCutoffDay() { return cutoffDay; }
    public void setCutoffDay(Integer cutoffDay) { this.cutoffDay = cutoffDay; }

    public String getSettlementTerms() { return settlementTerms; }
    public void setSettlementTerms(String settlementTerms) { this.settlementTerms = settlementTerms; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public PartnerStatus getStatus() { return status; }
    public void setStatus(PartnerStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Set<String> getBrands() { return brands; }
    public void setBrands(Set<String> brands) { this.brands = brands == null ? new LinkedHashSet<>() : new LinkedHashSet<>(brands); }
    public List<String> getPaymentCodeImages() { return paymentCodeImages; }
    public void setPaymentCodeImages(List<String> paymentCodeImages) {
        this.paymentCodeImages = paymentCodeImages == null ? new ArrayList<>() : new ArrayList<>(paymentCodeImages);
    }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public java.math.BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(java.math.BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public java.math.BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(java.math.BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public java.math.BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(java.math.BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
}
