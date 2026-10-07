package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.MaterialSourceType;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;

/**
 * 项目材料计划。
 *
 * <p>材料计划由套餐材料标准面积换算而来，记录材料需求量、计划到场时间和状态，
 * 为后续采购、到场、领用和材料成本提供项目级事实基础。</p>
 */
@Note("项目材料计划")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_material_plan",
        indexes = {
                @Index(name = "idx_material_plan_company", columnList = "com_id, plan_id"),
                @Index(name = "idx_material_plan_company_store", columnList = "com_id, store_id, plan_id"),
                @Index(name = "idx_material_plan_company_team", columnList = "com_id, team_id, plan_id"),
                @Index(name = "idx_material_plan_company_owner", columnList = "com_id, owner_id, plan_id"),
                @Index(name = "idx_material_plan_company_project", columnList = "com_id, project_id, plan_id"),
                @Index(name = "idx_material_plan_company_material_key", columnList = "com_id, material_key, plan_id"),
                @Index(name = "idx_material_plan_company_project_status", columnList = "com_id, project_id, status, plan_id")
        }
)
public class MaterialPlan extends AuditModel implements ProjectOwned {

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



    @Note("材料计划索引")
    @Id
    @SnowflakeId
    private Long planId;

    @Note("所属项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Note("合同基线")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ProjectContract contract;

    @Note("来源合同行")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_line_id")
    private ContractLine contractLine;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_item_id")
    @Note("来源变更行")
    private ContractChangeItem changeItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("关联施工任务")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private ProjectTask task;

    @Note("套餐分项")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_line_id")
    private PackageLine packageLine;

    @Note("设计报价材料分项")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_item_id")
    private QuoteLine quoteItem;

    @Note("报价项目库来源")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private CatalogItem catalogItem;

    @Note("计划数量")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Note("计量单位")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Unit unit = Unit.Area;

    @Note("参考单价")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Note("计划成本")
    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal cost = BigDecimal.ZERO;

    @Note("材料计划状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private MaterialPlanStatus status = MaterialPlanStatus.Planned;

    @Note("要求到场日期")
    private LocalDate requiredAt;

    @Note("实际到场日期")
    private LocalDate arrivedAt;

    @Note("材料计划来源：合同选材确认/增减项新增/增减项扣减")
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 24)
    private MaterialSourceType sourceType;

    @Note("确认后的选材结果版本")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selection_id")
    private MaterialSelection selection;

    @Note("选材品牌（结构化，用于供应商匹配）")
    @Column(name = "selected_brand", length = 128)
    private String selectedBrand;

    @Note("选材规格（结构化，用于供应商匹配）")
    @Column(name = "selected_spec", length = 256)
    private String selectedSpec;

    @Note("选材型号（结构化，用于供应商匹配）")
    @Column(name = "selected_model", length = 128)
    private String selectedModel;

    @Note("归一化材料标识（供应商匹配，不使用选材自由文本）")
    @Column(name = "material_key", length = 512)
    private String materialKey;

    @Note("净用量（施工净需求，空值表示沿用计划数量）")
    @Column(name = "net_quantity", precision = 12, scale = 2)
    private BigDecimal netQuantity;

    @Note("损耗率（来源主材分类，默认 0）")
    @Column(name = "loss_rate", precision = 8, scale = 4)
    private BigDecimal lossRate = BigDecimal.ZERO;

    @Note("损耗量；计划数量 = 净用量 + 损耗量")
    @Column(name = "loss_quantity", precision = 12, scale = 2)
    private BigDecimal lossQuantity = BigDecimal.ZERO;

    @Note("扣减行指向的被冲减原计划")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reduce_of_plan_id")
    private MaterialPlan reduceOfPlan;

    @Note("数量/价格调整原因")
    @Column(length = 500)
    private String remark;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ProjectTask getTask() {
        return task;
    }

    public void setTask(ProjectTask task) {
        this.task = task;
    }

    public QuoteLine getQuoteItem() { return quoteItem; }
    public void setQuoteItem(QuoteLine quoteItem) { this.quoteItem = quoteItem; }
    public PackageLine getPackageLine() {
        return packageLine;
    }

    public void setPackageLine(PackageLine packageLine) {
        this.packageLine = packageLine;
    }

    public CatalogItem getCatalogItem() { return catalogItem; }
    public void setCatalogItem(CatalogItem catalogItem) { this.catalogItem = catalogItem; }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public MaterialPlanStatus getStatus() {
        return status;
    }

    public void setStatus(MaterialPlanStatus status) {
        this.status = status;
    }

    public LocalDate getRequiredAt() {
        return requiredAt;
    }

    public void setRequiredAt(LocalDate requiredAt) {
        this.requiredAt = requiredAt;
    }

    public LocalDate getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(LocalDate arrivedAt) {
        this.arrivedAt = arrivedAt;
    }


    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public ContractLine getContractLine() { return contractLine; }
    public void setContractLine(ContractLine contractLine) { this.contractLine = contractLine; }
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


    public ContractChangeItem getChangeItem() { return changeItem; }

    public void setChangeItem(ContractChangeItem changeItem) { this.changeItem = changeItem; }

    public MaterialSourceType getSourceType() { return sourceType; }
    public void setSourceType(MaterialSourceType sourceType) { this.sourceType = sourceType; }
    public MaterialSelection getSelection() { return selection; }
    public void setSelection(MaterialSelection selection) { this.selection = selection; }
    public String getSelectedBrand() { return selectedBrand; }
    public void setSelectedBrand(String selectedBrand) { this.selectedBrand = selectedBrand; }
    public String getSelectedSpec() { return selectedSpec; }
    public void setSelectedSpec(String selectedSpec) { this.selectedSpec = selectedSpec; }
    public String getSelectedModel() { return selectedModel; }
    public void setSelectedModel(String selectedModel) { this.selectedModel = selectedModel; }
    public String getMaterialKey() { return materialKey; }
    public void setMaterialKey(String materialKey) { this.materialKey = materialKey; }
    public BigDecimal getNetQuantity() { return netQuantity; }
    public void setNetQuantity(BigDecimal netQuantity) { this.netQuantity = netQuantity; }
    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal lossRate) { this.lossRate = lossRate; }
    public BigDecimal getLossQuantity() { return lossQuantity; }
    public void setLossQuantity(BigDecimal lossQuantity) { this.lossQuantity = lossQuantity; }
    public MaterialPlan getReduceOfPlan() { return reduceOfPlan; }
    public void setReduceOfPlan(MaterialPlan reduceOfPlan) { this.reduceOfPlan = reduceOfPlan; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
