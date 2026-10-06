package com.th4.edge.decoboon.finance.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 实际业务付款。 */
@Note("业务付款")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_payment", indexes = {
        @Index(name = "idx_fin_payment_company", columnList = "com_id, payment_id"),
        @Index(name = "idx_fin_payment_project", columnList = "com_id, project_id, payment_id"),
        @Index(name = "idx_fin_payment_payable", columnList = "com_id, payable_id, payment_id"),
        @Index(name = "idx_fin_payment_store", columnList = "com_id, store_id, payment_id"),
        @Index(name = "idx_fin_payment_team", columnList = "com_id, team_id, payment_id"),
        @Index(name = "idx_fin_payment_owner", columnList = "com_id, owner_id, payment_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_fin_payment_company_idempotency",
                columnNames = {"com_id", "idempotency_key"})
})
public class Payment extends AuditModel {

    @Note("付款索引")
    @Id
    @SnowflakeId
    private Long paymentId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("归属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("归属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("数据负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Employee owner;

    @Note("所属项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Note("核销应付")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payable_id", nullable = false)
    private Payable payable;

    @Note("付款金额")
    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("付款账户")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private FundAccount account = FundAccount.CompanyBank;

    @Note("付款日期")
    @Column(nullable = false)
    private LocalDate paidAt;

    @Note("收款方")
    @Column(length = 128)
    private String payeeName;

    @Note("合作方（供应商/收款方）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private Partner partner;

    @Note("个人工人（工资发放时填写）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @Note("员工收款人（员工工资/提成发放时填写）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_emp_id")
    private Employee payeeEmployee;

    @Note("员工发放批次号")
    @Column(name = "batch_no", length = 64)
    private String batchNo;

    @Note("转账流水号")
    @Column(length = 128)
    private String transactionNo;

    @Note("凭证地址")
    @Column(length = 512)
    private String voucherUrl;

    @Note("付款备注")
    @Column(length = 500)
    private String remark;

    @Note("幂等键（客户端提交意图 ID，企业内唯一）")
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Note("业务指纹（用于校验同一幂等键是否对应同一笔业务）")
    @Column(name = "idempotency_fingerprint", length = 64)
    private String idempotencyFingerprint;

    @Note("经办人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Payable getPayable() { return payable; }
    public void setPayable(Payable payable) { this.payable = payable; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public FundAccount getAccount() { return account; }
    public void setAccount(FundAccount account) { this.account = account; }
    public LocalDate getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDate paidAt) { this.paidAt = paidAt; }
    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }
    public Worker getWorker() { return worker; }
    public void setWorker(Worker worker) { this.worker = worker; }
    public Employee getPayeeEmployee() { return payeeEmployee; }
    public void setPayeeEmployee(Employee payeeEmployee) { this.payeeEmployee = payeeEmployee; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    public String getVoucherUrl() { return voucherUrl; }
    public void setVoucherUrl(String voucherUrl) { this.voucherUrl = voucherUrl; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyFingerprint() { return idempotencyFingerprint; }
    public void setIdempotencyFingerprint(String idempotencyFingerprint) { this.idempotencyFingerprint = idempotencyFingerprint; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
