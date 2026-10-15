package com.th4.edge.decoboon.material.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 采购结算明细。
 *
 * <p>唯一约束 {@code (com_id, purchase_item_id)}：**同一采购明细只能被一种结算方式确认一次**，
 * 这是 DEC-073「互斥」的数据库级保障（月结不得重复计入已逐单结算的明细）。</p>
 */
@Note("主材采购结算明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_material_settlement_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_item_purchase_item",
                columnNames = {"com_id", "purchase_item_id"}),
        indexes = {
                @Index(name = "idx_settlement_item_company", columnList = "com_id, settlement_item_id"),
                @Index(name = "idx_settlement_item_company_settlement", columnList = "com_id, settlement_id")
        })
public class MaterialSettlementItem extends AuditModel {

    @Note("结算明细索引")
    @Id
    @SnowflakeId
    private Long settlementItemId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属结算单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private MaterialSettlement settlement;

    @Note("采购明细")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_item_id", nullable = false)
    private com.th4.edge.decoboon.project.model.PurchaseOrderItem purchaseItem;

    @Note("结算数量")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Note("结算价（实际成本唯一计价来源）")
    @Column(name = "settlement_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal settlementPrice = BigDecimal.ZERO;

    @Note("结算金额")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("备注")
    @Column(length = 500)
    private String remark;

    public Long getSettlementItemId() { return settlementItemId; }
    public void setSettlementItemId(Long settlementItemId) { this.settlementItemId = settlementItemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public MaterialSettlement getSettlement() { return settlement; }
    public void setSettlement(MaterialSettlement settlement) { this.settlement = settlement; }
    public com.th4.edge.decoboon.project.model.PurchaseOrderItem getPurchaseItem() { return purchaseItem; }
    public void setPurchaseItem(com.th4.edge.decoboon.project.model.PurchaseOrderItem purchaseItem) { this.purchaseItem = purchaseItem; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getSettlementPrice() { return settlementPrice; }
    public void setSettlementPrice(BigDecimal settlementPrice) { this.settlementPrice = settlementPrice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
