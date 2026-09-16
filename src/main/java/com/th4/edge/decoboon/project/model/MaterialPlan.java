package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
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
@Table(name = "db_decoboon_material_plan",
        indexes = {
                @Index(name = "idx_material_plan_company", columnList = "com_id, plan_id"),
                @Index(name = "idx_material_plan_company_store", columnList = "com_id, store_id, plan_id"),
                @Index(name = "idx_material_plan_company_team", columnList = "com_id, team_id, plan_id"),
                @Index(name = "idx_material_plan_company_owner", columnList = "com_id, owner_id, plan_id")
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
    @Column(length = 16, nullable = false)
    private MaterialPlanStatus status = MaterialPlanStatus.Planned;

    @Note("要求到场日期")
    private LocalDate requiredAt;

    @Note("实际到场日期")
    private LocalDate arrivedAt;

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


}
