package com.th4.edge.decoboon.catalog.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.CatalogSourceType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
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

/**
 * 企业统一家装施工材料库。
 *
 * <p>施工、主材、软装、电器、综合和全屋定制共用一条目录数据；
 * 设计师报价只能引用此目录，价格和公式由企业维护人员统一管理。</p>
 */
@Note("家装材料库")
@Description("企业统一维护的施工、主材、软装、电器和综合项目库")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "catalog_item", indexes = {
        @Index(name = "idx_catalog_company", columnList = "com_id, item_id"),
        @Index(name = "idx_catalog_company_type_category", columnList = "com_id, type, category, item_id"),
        @Index(name = "idx_catalog_company_type_brand", columnList = "com_id, type, category, brand, item_id"),
        @Index(name = "idx_catalog_company_maintainer", columnList = "com_id, maintainer_id, item_id")
})
public class CatalogItem extends AbstractModel {

    @Note("目录索引")
    @Id
    @SnowflakeId
    private Long itemId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("维护人员")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintainer_id")
    private Employee maintainer;

    @Note("目录类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ItemType type;

    @Note("项目编码")
    @Keyword
    @Column(length = 64)
    private String code;

    @Note("项目分类")
    @Keyword
    @Column(length = 64, nullable = false)
    private String category;

    @Note("品牌")
    @Keyword
    @Column(length = 128)
    private String brand;

    @Note("系列")
    @Keyword
    @Column(length = 128)
    private String series;

    @Note("规格")
    @Keyword
    @Column(length = 256)
    private String spec;

    @Note("项目名称")
    @Keyword
    @Column(length = 256, nullable = false)
    private String name;

    @Note("名称拼音首字母")
    @Keyword
    @Column(length = 64)
    private String nameInitials;

    // ---------- 类型特有明细（仅传输用，落库到对应明细表）----------
    @Note("施工工期(天)")
    @Transient
    private Integer durationDays;

    @Note("主材损耗率(%)")
    @Transient
    private BigDecimal lossRate;

    @Note("起订量")
    @Transient
    private BigDecimal minimumOrder;

    @Note("交期(天)")
    @Transient
    private Integer leadTimeDays;

    @Note("保修月数")
    @Transient
    private Integer warrantyMonths;

    @Note("柜体板材")
    @Transient
    private String board;

    @Note("台面")
    @Transient
    private String countertop;

    @Note("计量单位")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Unit unit = Unit.Item;

    @Note("计价公式")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private PricingMethod pricingMode = PricingMethod.MaterialAndManual;

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

    /**
     * 标准成本单价。
     *
     * <p>{@code null} = 成本待维护；{@code 0} = 明确零成本；{@code >0} = 已维护标准成本。
     * 不允许在导入或维护时把“待维护”写成 0。</p>
     */
    @Note("成本单价")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Note("计算公式说明")
    @Column(length = 256, nullable = false)
    private String formula;

    @Note("施工工艺")
    @Lob
    private String craft;

    @Note("工艺/验收标准")
    @Lob
    private String criterion;

    @Note("项目说明")
    @Lob
    private String description;

    @Note("效果图片")
    @ElementCollection
    @CollectionTable(name = "catalog_item_images", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "url", length = 512)
    private List<String> images;

    @Note("是否启用")
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Note("导入来源批次")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id")
    private CatalogImportBatch importBatch;

    @Note("来源类型")
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 16, nullable = false)
    private CatalogSourceType sourceType = CatalogSourceType.Manual;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Employee getMaintainer() { return maintainer; }
    public void setMaintainer(Employee maintainer) { this.maintainer = maintainer; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameInitials() { return nameInitials; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal lossRate) { this.lossRate = lossRate; }
    public BigDecimal getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(BigDecimal minimumOrder) { this.minimumOrder = minimumOrder; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public String getBoard() { return board; }
    public void setBoard(String board) { this.board = board; }
    public String getCountertop() { return countertop; }
    public void setCountertop(String countertop) { this.countertop = countertop; }

    public void setNameInitials(String nameInitials) { this.nameInitials = nameInitials; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public PricingMethod getPricingMode() { return pricingMode; }
    public void setPricingMode(PricingMethod pricingMode) { this.pricingMode = pricingMode; }
    public BigDecimal getMaterialPrice() { return materialPrice; }
    public void setMaterialPrice(BigDecimal materialPrice) { this.materialPrice = materialPrice; }
    public BigDecimal getManualPrice() { return manualPrice; }
    public void setManualPrice(BigDecimal manualPrice) { this.manualPrice = manualPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getFixedTotalPrice() { return fixedTotalPrice; }
    public void setFixedTotalPrice(BigDecimal fixedTotalPrice) { this.fixedTotalPrice = fixedTotalPrice; }
    public CatalogImportBatch getImportBatch() { return importBatch; }
    public void setImportBatch(CatalogImportBatch importBatch) { this.importBatch = importBatch; }
    public CatalogSourceType getSourceType() { return sourceType; }
    public void setSourceType(CatalogSourceType sourceType) { this.sourceType = sourceType; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public String getCraft() { return craft; }
    public void setCraft(String craft) { this.craft = craft; }
    public String getCriterion() { return criterion; }
    public void setCriterion(String criterion) { this.criterion = criterion; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
