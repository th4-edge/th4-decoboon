package com.th4.edge.decoboon.material.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.material.configure.SheetItemSource;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectOwned;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 主材下单作业单行。
 *
 * <p>行来源只有 {@link SheetItemSource#Plan}：必须挂在 {@link MaterialPlan} 上（合同选材确认 / 增减项新增）。
 * 合同外与标配外主材必须先走增减项，确认生成材料需求后再下单（DEC-072 第 6 项）；
 * "标配"是行的属性（{@code standardIncluded}），不是行来源，标配含但不额外计价。</p>
 *
 * <p>价格字段可为空（BR-045）：下单时可能没有价格，**不允许用 0 冒充**；
 * 报价成本价 {@code quoteCostPrice} 为只读快照，只用于预计成本与成本偏差对照。</p>
 */
@Note("主材下单作业单行")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_material_order_sheet_item", indexes = {
        @Index(name = "idx_mosi_company_sheet", columnList = "com_id, sheet_id, sort"),
        @Index(name = "idx_mosi_company_plan", columnList = "com_id, material_plan_id"),
        @Index(name = "idx_mosi_company_project", columnList = "com_id, project_id, item_id"),
        @Index(name = "idx_mosi_company", columnList = "com_id, item_id")
})
public class MaterialOrderSheetItem extends AuditModel implements ProjectOwned {

    @Note("作业单行索引")
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

    @Note("经营负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Employee owner;

    @Note("所属房屋")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Note("所属项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Note("所属作业单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", nullable = false)
    private MaterialOrderSheet sheet;

    @Note("行来源（仅 Plan）")
    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", length = 16, nullable = false)
    private SheetItemSource sourceKind = SheetItemSource.Plan;

    @Note("来源材料计划行")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_plan_id", nullable = false)
    private MaterialPlan materialPlan;

    @Note("是否套餐/合同标配含（标配不额外计价）")
    @Column(name = "standard_included", nullable = false)
    private Boolean standardIncluded = Boolean.FALSE;

    @Note("空间")
    @Column(name = "space_name", length = 128)
    private String spaceName;

    @Note("安装位置")
    @Column(name = "location_name", length = 128)
    private String locationName;

    @Note("材料名称")
    @Column(name = "material_name", length = 128)
    private String materialName;

    @Note("品牌")
    @Column(length = 128)
    private String brand;

    @Note("型号")
    @Column(length = 128)
    private String model;

    @Note("规格")
    @Column(length = 256)
    private String spec;

    @Note("颜色/花色")
    @Column(length = 128)
    private String color;

    @Note("尺寸")
    @Column(length = 128)
    private String size;

    @Note("工艺")
    @Column(length = 128)
    private String craft;

    @Note("玻璃")
    @Column(length = 128)
    private String glass;

    @Note("数量")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Note("单位")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Unit unit = Unit.Area;

    @Note("损耗率（继承主材分类，默认0）")
    @Column(name = "loss_rate", precision = 8, scale = 4, nullable = false)
    private BigDecimal lossRate = BigDecimal.ZERO;

    @Note("损耗量")
    @Column(name = "loss_quantity", precision = 12, scale = 2, nullable = false)
    private BigDecimal lossQuantity = BigDecimal.ZERO;

    @Note("报价成本价快照（只读，用于预计成本对照）")
    @Column(name = "quote_cost_price", precision = 12, scale = 2)
    private BigDecimal quoteCostPrice;

    @Note("采购价（可为空，禁止用0冒充）")
    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Note("结算价")
    @Column(name = "settlement_price", precision = 12, scale = 2)
    private BigDecimal settlementPrice;

    @Note("已下单数量（可分批/分供应商）")
    @Column(name = "ordered_quantity", precision = 12, scale = 2, nullable = false)
    private BigDecimal orderedQuantity = BigDecimal.ZERO;

    @Note("价格状态")
    @Enumerated(EnumType.STRING)
    @Column(name = "price_state", length = 16, nullable = false)
    private SheetPriceState priceState = SheetPriceState.Unpriced;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Note("备注")
    @Column(length = 500)
    private String remark;

    @Note("附件")
    @ElementCollection
    @CollectionTable(name = "db_material_order_sheet_item_attachments", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "url", length = 512)
    private List<String> attachments;

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
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public MaterialOrderSheet getSheet() { return sheet; }
    public void setSheet(MaterialOrderSheet sheet) { this.sheet = sheet; }
    public SheetItemSource getSourceKind() { return sourceKind; }
    public void setSourceKind(SheetItemSource sourceKind) { this.sourceKind = sourceKind; }
    public MaterialPlan getMaterialPlan() { return materialPlan; }
    public void setMaterialPlan(MaterialPlan materialPlan) { this.materialPlan = materialPlan; }
    public Boolean getStandardIncluded() { return standardIncluded; }
    public void setStandardIncluded(Boolean standardIncluded) { this.standardIncluded = standardIncluded; }
    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getCraft() { return craft; }
    public void setCraft(String craft) { this.craft = craft; }
    public String getGlass() { return glass; }
    public void setGlass(String glass) { this.glass = glass; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal lossRate) { this.lossRate = lossRate; }
    public BigDecimal getLossQuantity() { return lossQuantity; }
    public void setLossQuantity(BigDecimal lossQuantity) { this.lossQuantity = lossQuantity; }
    public BigDecimal getQuoteCostPrice() { return quoteCostPrice; }
    public void setQuoteCostPrice(BigDecimal quoteCostPrice) { this.quoteCostPrice = quoteCostPrice; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getSettlementPrice() { return settlementPrice; }
    public void setSettlementPrice(BigDecimal settlementPrice) { this.settlementPrice = settlementPrice; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(BigDecimal orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    public SheetPriceState getPriceState() { return priceState; }
    public void setPriceState(SheetPriceState priceState) { this.priceState = priceState; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
}
