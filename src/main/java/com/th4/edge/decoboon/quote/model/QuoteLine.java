package com.th4.edge.decoboon.quote.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceType;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
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

import java.math.BigDecimal;
import java.util.List;

/** 报价明细快照。 */
@Note("报价明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "quote_line", indexes = {
        @Index(name = "idx_quote_item_company", columnList = "com_id, item_id"),
        @Index(name = "idx_quote_item_company_store", columnList = "com_id, store_id, item_id"),
        @Index(name = "idx_quote_item_company_team", columnList = "com_id, team_id, item_id"),
        @Index(name = "idx_quote_item_company_owner", columnList = "com_id, emp_id, item_id"),
        @Index(name = "idx_quote_item_company_quote", columnList = "com_id, quote_id, item_id"),
        @Index(name = "idx_quote_item_company_space", columnList = "com_id, space_id, item_id")
})
public class QuoteLine extends AuditModel {

    @Note("明细索引")
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

    @Note("归属员工")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    private Employee owner;

    @Note("所属报价")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("设计分项来源")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_item_id")
    private DesignItem designItem;

    @Note("统一家装材料库来源")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private CatalogItem catalogItem;

    @Note("施工区域")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @Note("兼容套餐分项来源")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_line_id")
    private PackageLine packageLine;

    @Note("报价行角色")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private QuoteLineRole role = QuoteLineRole.Base;

    @Note("设计差异类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private QuoteDifferenceType differenceType = QuoteDifferenceType.None;

    @Note("设计差异状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private QuoteDifferenceStatus differenceStatus = QuoteDifferenceStatus.None;

    @Note("是否包含在基础报价中")
    @Column(nullable = false)
    private Boolean included = Boolean.TRUE;

    @Note("行折扣金额")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Note("价格与成本组件")
    @ElementCollection
    @CollectionTable(name = "quote_line_components", joinColumns = @JoinColumn(name = "line_id"))
    private List<QuoteComponent> components;

    @Note("分项类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ItemType type = ItemType.General;

    @Note("分项分类")
    @Keyword
    @Column(length = 64)
    private String category;

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

    @Note("材料单价")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal materialPrice = BigDecimal.ZERO;

    @Note("人工单价")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal manualPrice = BigDecimal.ZERO;

    @Note("固定总价（仅固定总价计价方式使用，独立于材料/人工单价）")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(name = "fixed_total_price", precision = 12, scale = 2)
    private BigDecimal fixedTotalPrice;

    @Note("计价公式")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private PricingMethod pricingMode = PricingMethod.MaterialAndManual;

    @Note("销售单价")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Note("分项金额")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("分项成本")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal cost = BigDecimal.ZERO;

    @Note("实际材料成本")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(name = "actual_material_cost", precision = 14, scale = 2, nullable = false)
    private BigDecimal actualMaterialCost = BigDecimal.ZERO;

    @Note("实际人工成本")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(name = "actual_labor_cost", precision = 14, scale = 2, nullable = false)
    private BigDecimal actualLaborCost = BigDecimal.ZERO;

    @Note("实际其他成本")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(name = "actual_other_cost", precision = 14, scale = 2, nullable = false)
    private BigDecimal actualOtherCost = BigDecimal.ZERO;

    @Note("实际总成本")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(name = "actual_cost", precision = 14, scale = 2, nullable = false)
    private BigDecimal actualCost = BigDecimal.ZERO;

    @Note("公式说明")
    @Column(length = 256)
    private String formula;

    @Note("工程量公式（Web Excel 式报价）")
    @Column(name = "quantity_formula", length = 512)
    private String quantityFormula;

    @Note("施工工艺快照（生成报价时从设计分项/材料库冻结）")
    @Lob
    private String craft;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

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
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public DesignItem getDesignItem() { return designItem; }
    public void setDesignItem(DesignItem designItem) { this.designItem = designItem; }
    public CatalogItem getCatalogItem() { return catalogItem; }
    public void setCatalogItem(CatalogItem catalogItem) { this.catalogItem = catalogItem; }
    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }
    public PackageLine getPackageLine() { return packageLine; }
    public void setPackageLine(PackageLine packageLine) { this.packageLine = packageLine; }
    public QuoteLineRole getRole() { return role; }
    public void setRole(QuoteLineRole role) { this.role = role; }
    public QuoteDifferenceType getDifferenceType() { return differenceType; }
    public void setDifferenceType(QuoteDifferenceType differenceType) { this.differenceType = differenceType; }
    public QuoteDifferenceStatus getDifferenceStatus() { return differenceStatus; }
    public void setDifferenceStatus(QuoteDifferenceStatus differenceStatus) { this.differenceStatus = differenceStatus; }
    public Boolean getIncluded() { return included; }
    public void setIncluded(Boolean included) { this.included = included; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public List<QuoteComponent> getComponents() { return components; }
    public void setComponents(List<QuoteComponent> components) { this.components = components; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
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
    public BigDecimal getMaterialPrice() { return materialPrice; }
    public void setMaterialPrice(BigDecimal materialPrice) { this.materialPrice = materialPrice; }
    public BigDecimal getManualPrice() { return manualPrice; }
    public void setManualPrice(BigDecimal manualPrice) { this.manualPrice = manualPrice; }
    public BigDecimal getFixedTotalPrice() { return fixedTotalPrice; }
    public void setFixedTotalPrice(BigDecimal fixedTotalPrice) { this.fixedTotalPrice = fixedTotalPrice; }
    public PricingMethod getPricingMode() { return pricingMode; }
    public void setPricingMode(PricingMethod pricingMode) { this.pricingMode = pricingMode; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getActualMaterialCost() { return actualMaterialCost; }
    public void setActualMaterialCost(BigDecimal actualMaterialCost) { this.actualMaterialCost = actualMaterialCost; }
    public BigDecimal getActualLaborCost() { return actualLaborCost; }
    public void setActualLaborCost(BigDecimal actualLaborCost) { this.actualLaborCost = actualLaborCost; }
    public BigDecimal getActualOtherCost() { return actualOtherCost; }
    public void setActualOtherCost(BigDecimal actualOtherCost) { this.actualOtherCost = actualOtherCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public String getQuantityFormula() { return quantityFormula; }
    public void setQuantityFormula(String quantityFormula) { this.quantityFormula = quantityFormula; }
    public String getCraft() { return craft; }
    public void setCraft(String craft) { this.craft = craft; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
}
