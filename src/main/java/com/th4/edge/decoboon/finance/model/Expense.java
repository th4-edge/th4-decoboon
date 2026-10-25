package com.th4.edge.decoboon.finance.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.finance.configure.ExpensePaymentMode;
import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 企业费用/员工报销业务事实。 */
@Note("费用报销单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_finance_expense",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fin_expense_company_no",
                        columnNames = {"com_id", "expense_no"}),
                @UniqueConstraint(name = "uk_fin_expense_company_idempotency",
                        columnNames = {"com_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_fin_expense_company", columnList = "com_id, expense_id"),
                @Index(name = "idx_fin_expense_store_status", columnList = "com_id, store_id, status, create_date_time"),
                @Index(name = "idx_fin_expense_requester", columnList = "com_id, requester_id, create_date_time"),
                @Index(name = "idx_fin_expense_project", columnList = "com_id, project_id, expense_date"),
                @Index(name = "idx_fin_expense_payee", columnList = "com_id, payee_emp_id, create_date_time"),
                @Index(name = "idx_fin_expense_owner", columnList = "com_id, owner_id, create_date_time"),
                @Index(name = "idx_fin_expense_company_payable", columnList = "com_id, payable_id")
        })
public class Expense extends AuditModel {

    @Id
    @SnowflakeId
    @Note("费用索引")
    private Long expenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("归属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Note("归属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @Note("所属项目；为空表示企业费用")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @Note("实际提交人")
    private Employee requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @Note("费用归属员工/数据负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_emp_id")
    @Note("报销收款员工")
    private Employee payeeEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @Note("费用分类")
    private ExpenseCategory category;

    @Keyword
    @Column(name = "expense_no", length = 64, nullable = false)
    @Note("费用编号")
    private String expenseNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 24, nullable = false)
    @Note("付款方式")
    private ExpensePaymentMode paymentMode = ExpensePaymentMode.Direct;

    @Unit("元")
    @Column(precision = 14, scale = 2, nullable = false)
    @Note("费用金额")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "expense_date", nullable = false)
    @Note("费用发生日期")
    private LocalDate expenseDate;

    @Lob
    @Column(nullable = false, columnDefinition = "longtext")
    @Note("费用说明")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("费用状态")
    private ExpenseStatus status = ExpenseStatus.Draft;

    @Column(nullable = false)
    @Note("提交版本")
    private Integer version = 0;

    @Column(name = "submitted_at")
    @Note("最近提交时间")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    @Note("审批人")
    private Employee approvedBy;

    @Column(name = "approved_at")
    @Note("审批时间")
    private LocalDateTime approvedAt;

    @Lob
    @Column(name = "approval_note", columnDefinition = "longtext")
    @Note("审批意见/退回原因")
    private String approvalNote;

    @Column(name = "self_approved", nullable = false)
    @Note("BOSS 一人企业自审标识")
    private Boolean selfApproved = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by_id")
    @Note("作废人")
    private Employee voidedBy;

    @Column(name = "voided_at")
    @Note("作废时间")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 500)
    @Note("作废原因")
    private String voidReason;

    @Column(name = "source_type", length = 32)
    @Note("来源类型")
    private String sourceType;

    @Column(name = "source_id")
    @Note("来源业务主键")
    private Long sourceId;

    @Column(name = "idempotency_key", length = 128, nullable = false)
    @Note("客户端幂等键")
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payable_id")
    @Note("审批通过后形成的费用应付")
    private Payable payable;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Note("证据附件")
    private List<ExpenseAttachment> attachments = new ArrayList<>();

    @Transient
    @Note("当前用户可执行动作")
    private List<String> allowedActions = new ArrayList<>();

    @Transient
    @Note("下一步动作")
    private String nextAction;

    @Transient
    @Note("提交版本记录")
    private List<ExpenseRevision> revisions = new ArrayList<>();

    @Transient
    @Note("付款记录")
    private List<Payment> payments = new ArrayList<>();

    @Transient
    @Note("已发行付款凭证")
    private List<VoucherRecord> vouchers = new ArrayList<>();

    @Transient
    @Note("付款状态")
    private String paymentStatus;

    @Transient
    @Unit("元")
    @Note("已付金额")
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Transient
    @Unit("元")
    @Note("未付金额")
    private BigDecimal unpaidAmount = BigDecimal.ZERO;

    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Employee getRequester() { return requester; }
    public void setRequester(Employee requester) { this.requester = requester; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Employee getPayeeEmployee() { return payeeEmployee; }
    public void setPayeeEmployee(Employee payeeEmployee) { this.payeeEmployee = payeeEmployee; }
    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }
    public String getExpenseNo() { return expenseNo; }
    public void setExpenseNo(String expenseNo) { this.expenseNo = expenseNo; }
    public ExpensePaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(ExpensePaymentMode paymentMode) { this.paymentMode = paymentMode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ExpenseStatus getStatus() { return status; }
    public void setStatus(ExpenseStatus status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public Employee getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Employee approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getApprovalNote() { return approvalNote; }
    public void setApprovalNote(String approvalNote) { this.approvalNote = approvalNote; }
    public Boolean getSelfApproved() { return selfApproved; }
    public void setSelfApproved(Boolean selfApproved) { this.selfApproved = selfApproved; }
    public Employee getVoidedBy() { return voidedBy; }
    public void setVoidedBy(Employee voidedBy) { this.voidedBy = voidedBy; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime voidedAt) { this.voidedAt = voidedAt; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Payable getPayable() { return payable; }
    public void setPayable(Payable payable) { this.payable = payable; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getUnpaidAmount() { return unpaidAmount; }
    public void setUnpaidAmount(BigDecimal unpaidAmount) { this.unpaidAmount = unpaidAmount; }
    public List<ExpenseAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<ExpenseAttachment> attachments) {
        this.attachments = attachments == null ? new ArrayList<>() : attachments;
    }
    public List<String> getAllowedActions() { return allowedActions; }
    public void setAllowedActions(List<String> allowedActions) {
        this.allowedActions = allowedActions == null ? new ArrayList<>() : allowedActions;
    }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public List<ExpenseRevision> getRevisions() { return revisions; }
    public void setRevisions(List<ExpenseRevision> revisions) {
        this.revisions = revisions == null ? new ArrayList<>() : revisions;
    }
    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) {
        this.payments = payments == null ? new ArrayList<>() : payments;
    }
    public List<VoucherRecord> getVouchers() { return vouchers; }
    public void setVouchers(List<VoucherRecord> vouchers) {
        this.vouchers = vouchers == null ? new ArrayList<>() : vouchers;
    }
}
