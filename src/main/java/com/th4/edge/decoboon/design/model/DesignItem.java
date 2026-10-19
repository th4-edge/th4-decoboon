package com.th4.edge.decoboon.design.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.design.configure.QuantitySource;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.field.annotation.LogicField;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.List;

/** 设计方案内的施工或材料分项。 */
@Note("设计分项")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_design_item", indexes = {
        @Index(name = "idx_design_item_company", columnList = "com_id, item_id"),
        @Index(name = "idx_design_item_company_store", columnList = "com_id, store_id, item_id"),
        @Index(name = "idx_design_item_company_team", columnList = "com_id, team_id, item_id"),
        @Index(name = "idx_design_item_company_owner", columnList = "com_id, emp_id, item_id"),
        @Index(name = "idx_design_item_company_design", columnList = "com_id, design_id, item_id"),
        @Index(name = "idx_design_item_company_space", columnList = "com_id, space_id, item_id")
})
public class DesignItem extends AuditModel {

    @Note("分项索引")
    @Id
    @SnowflakeId
    private Long itemId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("负责设计师")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee owner;

    @Note("所属方案")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @Note("施工区域，综合项目可为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("分项类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ItemType type = ItemType.Construction;

    @Note("分项分类")
    @Keyword
    @Column(length = 64, nullable = false)
    private String category;

    @Note("统一家装材料库来源")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private CatalogItem catalogItem;

    @Note("分项名称")
    @Keyword
    @Column(length = 128, nullable = false)
    private String name;

    @Note("品牌")
    @Column(length = 128)
    private String brand;

    @Note("规格")
    @Column(length = 128)
    private String spec;

    @Note("单位")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Unit unit = Unit.Area;

    @Note("工程量")
    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Note("工程量来源")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private QuantitySource quantitySource = QuantitySource.Formula;

    @Note("材料单价")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal materialPrice = BigDecimal.ZERO;

    @Note("人工单价")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal manualPrice = BigDecimal.ZERO;

    @Note("固定总价（仅固定总价计价方式使用）")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(name = "fixed_total_price", precision = 12, scale = 2)
    private BigDecimal fixedTotalPrice;

    @Note("成本单价（null = 成本待维护，0 = 明确零成本）")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Note("计价公式")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private PricingMethod pricingMode = PricingMethod.MaterialAndManual;

    @Note("分项金额")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @LogicField
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("分项成本")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @LogicField
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal cost = BigDecimal.ZERO;

    @Note("计价公式说明")
    @Column(length = 256)
    private String formula;

    @Note("工程量公式（专业变量）")
    @Column(name = "quantity_formula", length = 512)
    private String quantityFormula;

    /** 待补尺寸标记（DEC-058）：与 DesignTemplateFormulaContext 共用同一字面量。 */
    public static final String PENDING_MEASURE_MARKER = "__PENDING_MEASURE__";

    @Transient
    @Note("只读投影：待补尺寸")
    private Boolean pendingMeasure;

    @Transient
    @Note("只读投影：工程量公式待修订")
    private Boolean formulaRevisionNeeded;

    @Note("工艺说明")
    @Lob
    private String criterion;

    @Note("施工工艺快照（来自材料库，签约口径以分项为准）")
    @Lob
    private String craft;

    @Note("备注")
    @Column(length = 512)
    private String remark;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Note("设计效果图")
    @ElementCollection
    @CollectionTable(name = "db_design_item_images", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "url", length = 512)
    private List<String> images;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Design getDesign() { return design; }
    public void setDesign(Design design) { this.design = design; }
    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public CatalogItem getCatalogItem() { return catalogItem; }
    public void setCatalogItem(CatalogItem catalogItem) { this.catalogItem = catalogItem; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public QuantitySource getQuantitySource() { return quantitySource; }
    public void setQuantitySource(QuantitySource quantitySource) { this.quantitySource = quantitySource; }
    public BigDecimal getMaterialPrice() { return materialPrice; }
    public void setMaterialPrice(BigDecimal materialPrice) { this.materialPrice = materialPrice; }
    public BigDecimal getManualPrice() { return manualPrice; }
    public void setManualPrice(BigDecimal manualPrice) { this.manualPrice = manualPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getFixedTotalPrice() { return fixedTotalPrice; }
    public void setFixedTotalPrice(BigDecimal fixedTotalPrice) { this.fixedTotalPrice = fixedTotalPrice; }
    public PricingMethod getPricingMode() { return pricingMode; }
    public void setPricingMode(PricingMethod pricingMode) { this.pricingMode = pricingMode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public Boolean getPendingMeasure() {
        if (pendingMeasure != null) {
            return pendingMeasure;
        }
        // 由实例化写入的公式前缀派生（DEC-058）：0=待补 与 0=赠送 并存，不新增 DDL 字段。
        String effective = effectiveQuantityFormula();
        return effective != null && effective.startsWith(PENDING_MEASURE_MARKER + "|");
    }
    public void setPendingMeasure(Boolean pendingMeasure) { this.pendingMeasure = pendingMeasure; }
    public Boolean getFormulaRevisionNeeded() { return formulaRevisionNeeded; }
    public void setFormulaRevisionNeeded(Boolean formulaRevisionNeeded) {
        this.formulaRevisionNeeded = formulaRevisionNeeded;
    }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public String getQuantityFormula() { return quantityFormula; }
    public void setQuantityFormula(String quantityFormula) { this.quantityFormula = quantityFormula; }
    /**
     * 过渡兼容：拆分前实例化的待补分项把标记写在 formula；新数据只写 quantityFormula。
     * 只把待补标记回退读取，绝不把中文计价说明当作工程量公式。
     */
    public String effectiveQuantityFormula() {
        if (quantityFormula != null && !quantityFormula.isBlank()) {
            return quantityFormula;
        }
        return formula != null && formula.startsWith(PENDING_MEASURE_MARKER + "|") ? formula : null;
    }
    public String getCriterion() { return criterion; }
    public void setCriterion(String criterion) { this.criterion = criterion; }
    public String getCraft() { return craft; }
    public void setCraft(String craft) { this.craft = craft; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
}
