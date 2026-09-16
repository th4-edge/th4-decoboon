package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** 合同金额基线行，来源客户确认后的报价快照。 */
@Note("合同基线行")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_contract_line", indexes = {
        @Index(name = "idx_contract_line_company", columnList = "com_id, line_id"),
        @Index(name = "idx_contract_line_contract", columnList = "com_id, contract_id, line_id"),
        @Index(name = "idx_contract_line_property", columnList = "com_id, property_id, line_id")
,
        @Index(name = "idx_contract_line_company_store", columnList = "com_id, store_id, line_id"),
        @Index(name = "idx_contract_line_company_team", columnList = "com_id, team_id, line_id"),
        @Index(name = "idx_contract_line_company_owner", columnList = "com_id, owner_id, line_id")
})
public class ContractLine extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("合同行索引")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @Note("合同基线")
    private ProjectContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_quote_line_id", nullable = false)
    @Note("来源报价行")
    private QuoteLine sourceQuoteLine;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Note("项目类型")
    private ItemType type;

    @Column(length = 64, nullable = false)
    @Note("分类快照")
    private String category;

    @Column(length = 256, nullable = false)
    @Note("名称快照")
    private String name;

    @Column(length = 128)
    @Note("品牌快照")
    private String brand;

    @Column(length = 256)
    @Note("规格快照")
    private String spec;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Note("单位快照")
    private Unit unit = Unit.Area;

    @Column(precision = 12, scale = 3, nullable = false)
    @Note("数量")
    private BigDecimal quantity = BigDecimal.ZERO;

    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    @Note("材料单价快照")
    private BigDecimal materialPrice = BigDecimal.ZERO;

    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    @Note("人工单价快照")
    private BigDecimal manualPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("计价模式")
    private PricingMethod pricingMode = PricingMethod.MaterialAndManual;

    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    @Note("合同金额")
    private BigDecimal amount = BigDecimal.ZERO;

    @com.th4.edge.framework.core.annotation.Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    @Note("合同成本")
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(length = 256)
    @Note("公式快照")
    private String formula;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    @Note("合同行角色")
    private QuoteLineRole role = QuoteLineRole.Base;

    @Column(length = 128)
    @Note("区域名称")
    private String spaceName;

    @Column(nullable = false)
    @Note("排序")
    private Integer sort = 0;

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public QuoteLine getSourceQuoteLine() { return sourceQuoteLine; }
    public void setSourceQuoteLine(QuoteLine sourceQuoteLine) { this.sourceQuoteLine = sourceQuoteLine; }
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
    public PricingMethod getPricingMode() { return pricingMode; }
    public void setPricingMode(PricingMethod pricingMode) { this.pricingMode = pricingMode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public QuoteLineRole getRole() { return role; }
    public void setRole(QuoteLineRole role) { this.role = role; }
    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
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
