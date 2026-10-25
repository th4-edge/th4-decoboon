package com.th4.edge.decoboon.document.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 凭证记录。
 *
 * <p>收付款单据的打印载体：由来源资金流水（Ledger）生成，落库保存凭证号、
 * 金额、模板版本与 PDF 文件；重打读取同一条记录。
 * 身份证号与银行账号不落库为可检索列，完整值仅存在于 PDF 文件内。</p>
 */
@Note("凭证记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_voucher_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_voucher_company_no", columnNames = {"com_id", "voucher_no"}),
        indexes = {
                @Index(name = "idx_voucher_company", columnList = "com_id, voucher_id"),
                @Index(name = "idx_voucher_company_project", columnList = "com_id, project_id, voucher_id"),
                @Index(name = "idx_voucher_company_source", columnList = "com_id, source_type, source_id"),
                @Index(name = "idx_voucher_company_store", columnList = "com_id, store_id, voucher_id"),
                @Index(name = "idx_voucher_company_team", columnList = "com_id, team_id, voucher_id"),
                @Index(name = "idx_voucher_company_owner", columnList = "com_id, owner_id, voucher_id")
        })
public class VoucherRecord extends AuditModel {

    public static final String SOURCE_EXPENSE = "Expense";

    @Id @SnowflakeId @Note("凭证索引")
    private Long voucherId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") @Note("所属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id") @Note("所属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false) @Note("数据负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id") @Note("所属项目")
    private Project project;

    @Column(name = "source_type", length = 32) @Note("来源类型；费用凭证为 Expense")
    private String sourceType;

    @Column(name = "source_id") @Note("来源业务主键；费用凭证为 expenseId")
    private Long sourceId;

    @Column(name = "source_snapshot", columnDefinition = "LONGTEXT") @Note("发行时费用来源快照JSON")
    private String sourceSnapshot;

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("凭证类型")
    private DocumentType type = DocumentType.Payment;

    @Column(name = "voucher_no", length = 32, nullable = false) @Note("凭证编号")
    private String voucherNo;

    @Column(length = 128, nullable = false) @Note("凭证名称")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_id") @Note("单据模板")
    private DocumentTemplate template;

    @Column(nullable = false) @Note("模板版本")
    private Integer templateVersion = 1;

    @Unit("元") @Column(precision = 14, scale = 2, nullable = false) @Note("金额合计")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 128, nullable = false) @Note("金额大写")
    private String amountWords;

    @Column(length = 128) @Note("对方名称快照")
    private String partyName;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "worker_id") @Note("对方工人（个人工资）")
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "partner_id") @Note("对方合作方（班组/供应商）")
    private Partner partner;

    @Column(length = 512, nullable = false) @Note("私有对象键")
    private String path;

    @Column(length = 64, nullable = false) @Note("文件SHA256")
    private String sha256;

    @Column(length = 256, nullable = false) @Note("文件名")
    private String fileName;

    @Column(length = 64, nullable = false) @Note("内容类型")
    private String contentType = "application/pdf";

    @Column(nullable = false) @Note("文件大小")
    private Long size;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "generated_by_id", nullable = false) @Note("生成人")
    private Employee generatedBy;

    @Column(nullable = false) @Note("生成时间")
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("凭证状态")
    private DocumentStatus status = DocumentStatus.Active;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    @Note("凭证明细")
    private List<VoucherItem> items = new ArrayList<>();

    @Transient @Note("短期访问地址")
    private String url;

    @Transient @Note("短期地址过期时间")
    private LocalDateTime urlExpiresAt;

    @Transient @Note("费用编号快照")
    private String expenseNo;
    @Transient @Note("费用分类快照")
    private String expenseCategory;
    @Transient @Note("费用归属快照")
    private String expenseAttribution;
    @Transient @Note("费用提交人快照")
    private String expenseRequester;
    @Transient @Note("费用收款员工快照")
    private String expensePayee;
    @Transient @Note("费用审批版本快照")
    private Integer expenseApprovalVersion;
    @Transient @Note("关联付款索引（费用凭证读取投影）")
    private Long paymentId;
    @Transient @Note("发行模板预设编码")
    private String templatePresetCode;

    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
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
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public String getVoucherNo() { return voucherNo; }
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public DocumentTemplate getTemplate() { return template; }
    public void setTemplate(DocumentTemplate template) { this.template = template; }
    public Integer getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(Integer templateVersion) { this.templateVersion = templateVersion; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getAmountWords() { return amountWords; }
    public void setAmountWords(String amountWords) { this.amountWords = amountWords; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }
    public Worker getWorker() { return worker; }
    public void setWorker(Worker worker) { this.worker = worker; }
    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public Employee getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Employee generatedBy) { this.generatedBy = generatedBy; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public List<VoucherItem> getItems() { return items; }
    public void setItems(List<VoucherItem> items) { this.items = items == null ? new ArrayList<>() : items; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDateTime getUrlExpiresAt() { return urlExpiresAt; }
    public void setUrlExpiresAt(LocalDateTime urlExpiresAt) { this.urlExpiresAt = urlExpiresAt; }
    public String getExpenseNo() { return expenseNo; }
    public void setExpenseNo(String expenseNo) { this.expenseNo = expenseNo; }
    public String getExpenseCategory() { return expenseCategory; }
    public void setExpenseCategory(String expenseCategory) { this.expenseCategory = expenseCategory; }
    public String getExpenseAttribution() { return expenseAttribution; }
    public void setExpenseAttribution(String expenseAttribution) { this.expenseAttribution = expenseAttribution; }
    public String getExpenseRequester() { return expenseRequester; }
    public void setExpenseRequester(String expenseRequester) { this.expenseRequester = expenseRequester; }
    public String getExpensePayee() { return expensePayee; }
    public void setExpensePayee(String expensePayee) { this.expensePayee = expensePayee; }
    public Integer getExpenseApprovalVersion() { return expenseApprovalVersion; }
    public void setExpenseApprovalVersion(Integer expenseApprovalVersion) { this.expenseApprovalVersion = expenseApprovalVersion; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getTemplatePresetCode() { return templatePresetCode; }
    public void setTemplatePresetCode(String templatePresetCode) { this.templatePresetCode = templatePresetCode; }
}
