package com.th4.edge.decoboon.material.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.material.configure.SettlementScope;
import com.th4.edge.decoboon.material.configure.SettlementStatus;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 主材采购结算单（P3，BR-045 / DEC-073）。
 *
 * <p>结算价是**实际成本的唯一计价来源**：确认后回填采购明细的结算价、把暂估应付修正为确定金额，
 * 并把差额补记到报价行实际成本，使"无价下单"的成本在结算收敛。</p>
 *
 * <p>结算价**不沉淀**为供应商参考价（DEC-072 第 5 项），也不回溯改写报价与合同快照。</p>
 */
@Note("主材采购结算单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_material_settlement",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_company_no", columnNames = {"com_id", "no"}),
        indexes = {
                @Index(name = "idx_settlement_company", columnList = "com_id, settlement_id"),
                @Index(name = "idx_settlement_company_supplier", columnList = "com_id, supplier_id, settlement_id"),
                @Index(name = "idx_settlement_company_scope_period", columnList = "com_id, scope, period"),
                @Index(name = "idx_settlement_company_store", columnList = "com_id, store_id, settlement_id"),
                @Index(name = "idx_settlement_company_team", columnList = "com_id, team_id, settlement_id"),
                @Index(name = "idx_settlement_company_owner", columnList = "com_id, owner_id, settlement_id")
        })
public class MaterialSettlement extends AuditModel {

    @Note("结算单索引")
    @Id
    @SnowflakeId
    private Long settlementId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private com.th4.edge.tenant.user.model.Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private com.th4.edge.tenant.user.model.Team team;

    @Note("经营负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Employee owner;

    @Note("所属项目（月结跨多项目时为空）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private com.th4.edge.decoboon.project.model.Project project;

    @Note("结算单号")
    @Column(length = 64, nullable = false)
    private String no;

    @Note("结算方式")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private SettlementScope scope = SettlementScope.PerPurchaseOrder;

    @Note("结算期间（月结使用，如 2026-10）")
    @Column(length = 32)
    private String period;

    @Note("供应商")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Partner supplier;

    @Note("结算日期")
    @Column(name = "settled_at")
    private LocalDate settledAt;

    @Note("结算金额（Σ 结算价 × 数量）")
    @Column(name = "total_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Note("结算单状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private SettlementStatus status = SettlementStatus.Confirmed;

    @Note("备注")
    @Column(length = 500)
    private String remark;

    @Note("确认人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmer_id", nullable = false)
    private Employee confirmer;

    @jakarta.persistence.Transient
    @Note("结算明细（响应投影，不落库）")
    private List<MaterialSettlementItem> items;

    public com.th4.edge.tenant.user.model.Store getStore() { return store; }
    public void setStore(com.th4.edge.tenant.user.model.Store store) { this.store = store; }
    public com.th4.edge.tenant.user.model.Team getTeam() { return team; }
    public void setTeam(com.th4.edge.tenant.user.model.Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public com.th4.edge.decoboon.project.model.Project getProject() { return project; }
    public void setProject(com.th4.edge.decoboon.project.model.Project project) { this.project = project; }

    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public SettlementScope getScope() { return scope; }
    public void setScope(SettlementScope scope) { this.scope = scope; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Partner getSupplier() { return supplier; }
    public void setSupplier(Partner supplier) { this.supplier = supplier; }
    public LocalDate getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDate settledAt) { this.settledAt = settledAt; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Employee getConfirmer() { return confirmer; }
    public void setConfirmer(Employee confirmer) { this.confirmer = confirmer; }
    public List<MaterialSettlementItem> getItems() { return items; }
    public void setItems(List<MaterialSettlementItem> items) { this.items = items; }
}
