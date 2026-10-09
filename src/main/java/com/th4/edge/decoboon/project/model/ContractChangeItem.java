package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 变更报价明细。 */
@Note("变更报价明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_contract_change_item", indexes = {
        @Index(name = "idx_change_item_company", columnList = "com_id, change_item_id"),
        @Index(name = "idx_change_item_change", columnList = "com_id, change_id, change_item_id"),
        @Index(name = "idx_contract_change_item_company", columnList = "com_id, change_item_id"),
        @Index(name = "idx_contract_change_item_company_store", columnList = "com_id, store_id, change_item_id"),
        @Index(name = "idx_contract_change_item_company_team", columnList = "com_id, team_id, change_item_id"),
        @Index(name = "idx_contract_change_item_company_owner", columnList = "com_id, owner_id, change_item_id"),
        @Index(name = "idx_change_item_company_source_line", columnList = "com_id, contract_line_id, change_item_id"),
        @Index(name = "idx_change_item_company_source_change", columnList = "com_id, source_change_item_id, change_item_id"),
        @Index(name = "idx_change_item_company_target_catalog", columnList = "com_id, target_catalog_item_id, change_item_id")
})
public class ContractChangeItem extends AuditModel implements ProjectOwned {
    @Id @SnowflakeId @Note("明细索引") private Long changeItemId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;

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

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "change_id", nullable = false) @Note("变更单") private ContractChange change;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "contract_line_id") @Note("原合同行来源") private ContractLine sourceLine;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_change_item_id") @Note("已生效变更目标来源") private ContractChangeItem sourceChangeItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "target_catalog_item_id") @Note("目标报价项目") private CatalogItem targetCatalogItem;

    @Enumerated(EnumType.STRING) @Column(length = 20, nullable = false) @Note("变更类型") private ContractChangeType changeType;
    @Enumerated(EnumType.STRING) @Column(length = 20) @Note("项目类型") private ItemType itemType;
    @Column(length = 256, nullable = false) @Note("项目名称") private String name;
    @Column(precision = 12, scale = 3, nullable = false) @Note("原数量") private BigDecimal oldQuantity = BigDecimal.ZERO;
    @Column(precision = 12, scale = 3, nullable = false) @Note("新数量") private BigDecimal newQuantity = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 12, scale = 2, nullable = false) @Note("原单价") private BigDecimal oldPrice = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 12, scale = 2, nullable = false) @Note("新单价") private BigDecimal newPrice = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 14, scale = 2, nullable = false) @Note("金额变化") private BigDecimal amountDelta = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 14, scale = 2) @Note("成本变化；null 表示目标成本未完整") private BigDecimal costDelta;
    @Column(length = 500) @Note("说明") private String remark;

    // ---------- 创建时来源基线快照 ----------
    @Column(name = "source_remaining_quantity", precision = 12, scale = 3) @Note("来源当前有效剩余工程量")
    private BigDecimal sourceRemainingQuantity;
    @Unit("元") @Column(name = "source_remaining_amount", precision = 14, scale = 2) @Note("来源当前有效剩余金额")
    private BigDecimal sourceRemainingAmount;
    @Unit("元") @Column(name = "source_unit_price", precision = 12, scale = 2) @Note("来源当前有效单价")
    private BigDecimal sourceUnitPrice;
    @Column(name = "source_baseline_time") @Note("来源基线时间")
    private LocalDateTime sourceBaselineTime;

    // ---------- 目标报价项目业务快照 ----------
    @Column(name = "target_name", length = 256) @Note("目标名称快照")
    private String targetName;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", length = 20) @Note("目标类型快照")
    private ItemType targetType;
    @Column(name = "target_category", length = 64) @Note("目标分类快照")
    private String targetCategory;
    @Column(name = "target_brand", length = 128) @Note("目标品牌快照")
    private String targetBrand;
    @Column(name = "target_series", length = 128) @Note("目标系列快照")
    private String targetSeries;
    @Column(name = "target_spec", length = 256) @Note("目标规格快照")
    private String targetSpec;
    @Enumerated(EnumType.STRING) @Column(name = "target_unit", length = 32) @Note("目标单位快照")
    private com.th4.edge.decoboon.bim.configure.Unit targetUnit;
    @Enumerated(EnumType.STRING) @Column(name = "target_pricing_mode", length = 24) @Note("目标计价方式")
    private PricingMethod targetPricingMode;
    @Unit("元") @Column(name = "target_material_price", precision = 12, scale = 2) @Note("目标材料单价")
    private BigDecimal targetMaterialPrice;
    @Unit("元") @Column(name = "target_manual_price", precision = 12, scale = 2) @Note("目标人工单价")
    private BigDecimal targetManualPrice;
    @Unit("元") @Column(name = "target_fixed_total_price", precision = 12, scale = 2) @Note("目标固定总价")
    private BigDecimal targetFixedTotalPrice;
    @Column(name = "target_formula", length = 256) @Note("目标公式快照")
    private String targetFormula;
    @Lob @Column(name = "target_craft", columnDefinition = "longtext") @Note("目标施工工艺")
    private String targetCraft;
    @Lob @Column(name = "target_criterion", columnDefinition = "longtext") @Note("目标验收标准")
    private String targetCriterion;
    @Column(name = "target_quantity", precision = 12, scale = 3) @Note("目标工程量")
    private BigDecimal targetQuantity;
    @Unit("元") @Column(name = "target_amount", precision = 14, scale = 2) @Note("目标金额")
    private BigDecimal targetAmount;
    @Unit("元") @Column(name = "target_cost_price", precision = 12, scale = 2) @Note("目标成本单价；null=待维护")
    private BigDecimal targetCostPrice;
    @Unit("元") @Column(name = "target_cost_amount", precision = 14, scale = 2) @Note("目标成本金额；null=待维护")
    private BigDecimal targetCostAmount;
    @Column(name = "target_cost_complete", nullable = false) @Note("目标成本是否完整")
    private Boolean targetCostComplete = Boolean.FALSE;

    /** 批量录入绑定用：原合同行索引；不落库，落库仍走 sourceLine 关联。 */
    @Transient @Note("原合同行索引") private Long sourceLineId;

    /** 批量录入绑定用：已生效变更目标来源索引；不落库，落库走 sourceChangeItem 关联。 */
    @Transient @Note("已生效变更目标来源索引") private Long sourceChangeItemId;

    /** 批量录入绑定用：目标报价项目索引；不落库，落库走 targetCatalogItem 关联。 */
    @Transient @Note("目标报价项目索引") private Long targetCatalogItemId;

    /** 新口径减项输入：本次减少工程量；不落库，落库转换为 old/new quantity。 */
    @Transient @Note("本次减少工程量") private BigDecimal reduceQuantity;

    /** 兼容历史批量录入：手填新成本；新口径成本只来自目标报价项目快照。 */
    @Transient @Note("兼容新成本") private BigDecimal newCost;

    public Long getChangeItemId() { return changeItemId; }
    public void setChangeItemId(Long changeItemId) { this.changeItemId = changeItemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ContractChange getChange() { return change; }
    public void setChange(ContractChange change) { this.change = change; }
    public ContractLine getSourceLine() { return sourceLine; }
    public void setSourceLine(ContractLine sourceLine) { this.sourceLine = sourceLine; }
    public ContractChangeItem getSourceChangeItem() { return sourceChangeItem; }
    public void setSourceChangeItem(ContractChangeItem sourceChangeItem) { this.sourceChangeItem = sourceChangeItem; }
    public CatalogItem getTargetCatalogItem() { return targetCatalogItem; }
    public void setTargetCatalogItem(CatalogItem targetCatalogItem) { this.targetCatalogItem = targetCatalogItem; }
    public ContractChangeType getChangeType() { return changeType; }
    public void setChangeType(ContractChangeType changeType) { this.changeType = changeType; }
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getOldQuantity() { return oldQuantity; }
    public void setOldQuantity(BigDecimal oldQuantity) { this.oldQuantity = oldQuantity; }
    public BigDecimal getNewQuantity() { return newQuantity; }
    public void setNewQuantity(BigDecimal newQuantity) { this.newQuantity = newQuantity; }
    public BigDecimal getOldPrice() { return oldPrice; }
    public void setOldPrice(BigDecimal oldPrice) { this.oldPrice = oldPrice; }
    public BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
    public BigDecimal getAmountDelta() { return amountDelta; }
    public void setAmountDelta(BigDecimal amountDelta) { this.amountDelta = amountDelta; }
    public BigDecimal getCostDelta() { return costDelta; }
    public void setCostDelta(BigDecimal costDelta) { this.costDelta = costDelta; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public BigDecimal getSourceRemainingQuantity() { return sourceRemainingQuantity; }
    public void setSourceRemainingQuantity(BigDecimal sourceRemainingQuantity) { this.sourceRemainingQuantity = sourceRemainingQuantity; }
    public BigDecimal getSourceRemainingAmount() { return sourceRemainingAmount; }
    public void setSourceRemainingAmount(BigDecimal sourceRemainingAmount) { this.sourceRemainingAmount = sourceRemainingAmount; }
    public BigDecimal getSourceUnitPrice() { return sourceUnitPrice; }
    public void setSourceUnitPrice(BigDecimal sourceUnitPrice) { this.sourceUnitPrice = sourceUnitPrice; }
    public LocalDateTime getSourceBaselineTime() { return sourceBaselineTime; }
    public void setSourceBaselineTime(LocalDateTime sourceBaselineTime) { this.sourceBaselineTime = sourceBaselineTime; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public ItemType getTargetType() { return targetType; }
    public void setTargetType(ItemType targetType) { this.targetType = targetType; }
    public String getTargetCategory() { return targetCategory; }
    public void setTargetCategory(String targetCategory) { this.targetCategory = targetCategory; }
    public String getTargetBrand() { return targetBrand; }
    public void setTargetBrand(String targetBrand) { this.targetBrand = targetBrand; }
    public String getTargetSeries() { return targetSeries; }
    public void setTargetSeries(String targetSeries) { this.targetSeries = targetSeries; }
    public String getTargetSpec() { return targetSpec; }
    public void setTargetSpec(String targetSpec) { this.targetSpec = targetSpec; }
    public com.th4.edge.decoboon.bim.configure.Unit getTargetUnit() { return targetUnit; }
    public void setTargetUnit(com.th4.edge.decoboon.bim.configure.Unit targetUnit) { this.targetUnit = targetUnit; }
    public PricingMethod getTargetPricingMode() { return targetPricingMode; }
    public void setTargetPricingMode(PricingMethod targetPricingMode) { this.targetPricingMode = targetPricingMode; }
    public BigDecimal getTargetMaterialPrice() { return targetMaterialPrice; }
    public void setTargetMaterialPrice(BigDecimal targetMaterialPrice) { this.targetMaterialPrice = targetMaterialPrice; }
    public BigDecimal getTargetManualPrice() { return targetManualPrice; }
    public void setTargetManualPrice(BigDecimal targetManualPrice) { this.targetManualPrice = targetManualPrice; }
    public BigDecimal getTargetFixedTotalPrice() { return targetFixedTotalPrice; }
    public void setTargetFixedTotalPrice(BigDecimal targetFixedTotalPrice) { this.targetFixedTotalPrice = targetFixedTotalPrice; }
    public String getTargetFormula() { return targetFormula; }
    public void setTargetFormula(String targetFormula) { this.targetFormula = targetFormula; }
    public String getTargetCraft() { return targetCraft; }
    public void setTargetCraft(String targetCraft) { this.targetCraft = targetCraft; }
    public String getTargetCriterion() { return targetCriterion; }
    public void setTargetCriterion(String targetCriterion) { this.targetCriterion = targetCriterion; }
    public BigDecimal getTargetQuantity() { return targetQuantity; }
    public void setTargetQuantity(BigDecimal targetQuantity) { this.targetQuantity = targetQuantity; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public BigDecimal getTargetCostPrice() { return targetCostPrice; }
    public void setTargetCostPrice(BigDecimal targetCostPrice) { this.targetCostPrice = targetCostPrice; }
    public BigDecimal getTargetCostAmount() { return targetCostAmount; }
    public void setTargetCostAmount(BigDecimal targetCostAmount) { this.targetCostAmount = targetCostAmount; }
    public Boolean getTargetCostComplete() { return targetCostComplete; }
    public void setTargetCostComplete(Boolean targetCostComplete) { this.targetCostComplete = targetCostComplete; }
    public Long getSourceLineId() { return sourceLineId; }
    public void setSourceLineId(Long sourceLineId) { this.sourceLineId = sourceLineId; }
    public Long getSourceChangeItemId() { return sourceChangeItemId; }
    public void setSourceChangeItemId(Long sourceChangeItemId) { this.sourceChangeItemId = sourceChangeItemId; }
    public Long getTargetCatalogItemId() { return targetCatalogItemId; }
    public void setTargetCatalogItemId(Long targetCatalogItemId) { this.targetCatalogItemId = targetCatalogItemId; }
    public BigDecimal getReduceQuantity() { return reduceQuantity; }
    public void setReduceQuantity(BigDecimal reduceQuantity) { this.reduceQuantity = reduceQuantity; }
    public BigDecimal getNewCost() { return newCost; }
    public void setNewCost(BigDecimal newCost) { this.newCost = newCost; }

    @Override
    public Store getStore() { return store; }

    @Override
    public void setStore(Store store) { this.store = store; }

    @Override
    public Team getTeam() { return team; }

    @Override
    public void setTeam(Team team) { this.team = team; }

    @Override
    public Employee getOwner() { return owner; }

    @Override
    public void setOwner(Employee owner) { this.owner = owner; }
}
