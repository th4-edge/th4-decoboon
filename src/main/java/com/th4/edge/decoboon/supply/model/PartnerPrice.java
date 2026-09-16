package com.th4.edge.decoboon.supply.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 合作方项目价格与交期。 */
@Note("合作方价格")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_decoboon_partner_price", indexes = {
        @Index(name = "idx_partner_price_company", columnList = "com_id, partner_price_id"),
        @Index(name = "idx_partner_price_partner", columnList = "com_id, partner_id, partner_price_id"),
        @Index(name = "idx_partner_price_item", columnList = "com_id, item_id, partner_price_id")
})
public class PartnerPrice extends AuditModel {

    @Id @SnowflakeId @Note("价格索引") private Long partnerPriceId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "partner_id", nullable = false) @Note("合作方") private Partner partner;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "item_id", nullable = false) @Note("项目库") private CatalogItem item;
    @Unit("元") @Column(precision = 12, scale = 2, nullable = false) @Note("采购价") private BigDecimal purchasePrice = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 12, scale = 2, nullable = false) @Note("人工/班组价") private BigDecimal laborPrice = BigDecimal.ZERO;
    @Column(precision = 12, scale = 3, nullable = false) @Note("最小起订量") private BigDecimal minimumOrder = BigDecimal.ONE;
    @Column(nullable = false) @Note("交货天数") private Integer leadTimeDays = 0;
    @Column(nullable = false) @Note("账期天数") private Integer settlementDays = 0;
    @Note("生效日期") private LocalDate effectiveFrom;
    @Note("失效日期") private LocalDate effectiveTo;
    @Column(nullable = false) @Note("是否启用") private Boolean active = Boolean.TRUE;
    @Column(length = 500) @Note("备注") private String remark;

    public Long getPartnerPriceId() { return partnerPriceId; }
    public void setPartnerPriceId(Long partnerPriceId) { this.partnerPriceId = partnerPriceId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }
    public CatalogItem getItem() { return item; }
    public void setItem(CatalogItem item) { this.item = item; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getLaborPrice() { return laborPrice; }
    public void setLaborPrice(BigDecimal laborPrice) { this.laborPrice = laborPrice; }
    public BigDecimal getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(BigDecimal minimumOrder) { this.minimumOrder = minimumOrder; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public Integer getSettlementDays() { return settlementDays; }
    public void setSettlementDays(Integer settlementDays) { this.settlementDays = settlementDays; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
