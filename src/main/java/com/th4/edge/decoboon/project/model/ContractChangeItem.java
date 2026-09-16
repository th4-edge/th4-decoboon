package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.catalog.configure.ItemType;
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

/** 变更报价明细。 */
@Note("变更报价明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_contract_change_item", indexes = {
        @Index(name = "idx_change_item_company", columnList = "com_id, change_item_id"),
        @Index(name = "idx_change_item_change", columnList = "com_id, change_id, change_item_id")
,
        @Index(name = "idx_contract_change_item_company", columnList = "com_id, change_item_id"),
        @Index(name = "idx_contract_change_item_company_store", columnList = "com_id, store_id, change_item_id"),
        @Index(name = "idx_contract_change_item_company_team", columnList = "com_id, team_id, change_item_id"),
        @Index(name = "idx_contract_change_item_company_owner", columnList = "com_id, owner_id, change_item_id")
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
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "contract_line_id") @Note("原合同行") private ContractLine sourceLine;
    @Enumerated(EnumType.STRING) @Column(length = 20, nullable = false) @Note("变更类型") private ContractChangeType changeType;
    @Enumerated(EnumType.STRING) @Column(length = 20) @Note("项目类型") private ItemType itemType;
    @Column(length = 256, nullable = false) @Note("项目名称") private String name;
    @Column(precision = 12, scale = 3, nullable = false) @Note("原数量") private BigDecimal oldQuantity = BigDecimal.ZERO;
    @Column(precision = 12, scale = 3, nullable = false) @Note("新数量") private BigDecimal newQuantity = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 12, scale = 2, nullable = false) @Note("原单价") private BigDecimal oldPrice = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 12, scale = 2, nullable = false) @Note("新单价") private BigDecimal newPrice = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 14, scale = 2, nullable = false) @Note("金额变化") private BigDecimal amountDelta = BigDecimal.ZERO;
    @Unit("元") @Column(precision = 14, scale = 2, nullable = false) @Note("成本变化") private BigDecimal costDelta = BigDecimal.ZERO;
    @Column(length = 500) @Note("说明") private String remark;

    public Long getChangeItemId() { return changeItemId; }
    public void setChangeItemId(Long changeItemId) { this.changeItemId = changeItemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ContractChange getChange() { return change; }
    public void setChange(ContractChange change) { this.change = change; }
    public ContractLine getSourceLine() { return sourceLine; }
    public void setSourceLine(ContractLine sourceLine) { this.sourceLine = sourceLine; }
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
