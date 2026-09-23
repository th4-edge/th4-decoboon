package com.th4.edge.decoboon.supply.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 材料-供应商供应关系。
 *
 * <p>按 DEC-MP-03/DEC-MP-08：主材与供应商的关联沉淀在材料/供应链域，
 * 不落在报价项目库；材料标识只使用结构化字段（目录项 + 品牌 + 规格 + 型号 + 单位），
 * 不使用选材自由文本。同一材料可有多个供应商，供财务在下单时选择。</p>
 */
@Note("材料-供应商供应关系")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_material_supplier", indexes = {
        @Index(name = "idx_material_supplier_company", columnList = "com_id, material_supplier_id"),
        @Index(name = "idx_material_supplier_company_key", columnList = "com_id, material_key, material_supplier_id"),
        @Index(name = "idx_material_supplier_company_partner", columnList = "com_id, partner_id, material_supplier_id")
})
public class MaterialSupplier extends AuditModel {

    @Id
    @SnowflakeId
    @Note("材料供应关系索引")
    private Long materialSupplierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    @Note("供应商")
    private Partner partner;

    @Note("归一化材料标识（结构化字段拼接，不使用选材自由文本）")
    @Column(name = "material_key", length = 512, nullable = false)
    private String materialKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    @Note("报价项目库来源（可空）")
    private CatalogItem catalogItem;

    @Column(length = 128)
    @Note("材料名称")
    private String materialName;

    @Column(length = 128)
    @Note("品牌")
    private String brand;

    @Column(length = 256)
    @Note("规格")
    private String spec;

    @Column(length = 128)
    @Note("型号")
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    @Note("单位")
    private Unit unit;

    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(name = "purchase_price", precision = 12, scale = 2, nullable = false)
    @Note("参考采购价")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "minimum_order", precision = 12, scale = 3, nullable = false)
    @Note("最小起订量")
    private BigDecimal minimumOrder = BigDecimal.ONE;

    @Column(name = "lead_time_days", nullable = false)
    @Note("交货天数")
    private Integer leadTimeDays = 0;

    @Column(name = "settlement_days")
    @Note("账期天数（下单时快照到应付）")
    private Integer settlementDays;

    @Note("生效日期")
    private LocalDate effectiveFrom;

    @Note("失效日期")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    @Note("是否启用")
    private Boolean active = Boolean.TRUE;

    @Column(length = 500)
    @Note("备注")
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @Note("创建人")
    private Employee createdBy;

    public Long getMaterialSupplierId() { return materialSupplierId; }
    public void setMaterialSupplierId(Long materialSupplierId) { this.materialSupplierId = materialSupplierId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }
    public String getMaterialKey() { return materialKey; }
    public void setMaterialKey(String materialKey) { this.materialKey = materialKey; }
    public CatalogItem getCatalogItem() { return catalogItem; }
    public void setCatalogItem(CatalogItem catalogItem) { this.catalogItem = catalogItem; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
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
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
