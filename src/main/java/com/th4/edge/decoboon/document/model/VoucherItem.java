package com.th4.edge.decoboon.document.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
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
import java.time.LocalDate;

/**
 * 凭证明细行。
 *
 * <p>一行对应一笔来源资金流水，保留来源单号以便回溯；
 * 到账账户等敏感值不落库，仅存在于凭证 PDF 内。</p>
 */
@Note("凭证明细")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_voucher_item", indexes = {
        @Index(name = "idx_voucher_item_company", columnList = "com_id, item_id"),
        @Index(name = "idx_voucher_item_voucher", columnList = "voucher_id, item_id"),
        @Index(name = "idx_voucher_item_source", columnList = "com_id, source_type, source_id")
})
public class VoucherItem extends AuditModel {

    @Id @SnowflakeId @Note("明细索引")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "voucher_id", nullable = false) @Note("所属凭证")
    private VoucherRecord voucher;

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("来源款项类型")
    private DocumentType sourceType = DocumentType.Payment;

    @Column(nullable = false) @Note("来源款项索引")
    private Long sourceId;

    @Column(length = 64, nullable = false) @Note("来源单号")
    private String sourceNo;

    @Column(nullable = false) @Note("业务日期")
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("支付/收款账户")
    private FundAccount account = FundAccount.CompanyBank;

    @Unit("元") @Column(precision = 14, scale = 2, nullable = false) @Note("金额")
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 32) @Note("款项类型")
    private String category;

    @Column(length = 500) @Note("备注")
    private String remark;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public VoucherRecord getVoucher() { return voucher; }
    public void setVoucher(VoucherRecord voucher) { this.voucher = voucher; }
    public DocumentType getSourceType() { return sourceType; }
    public void setSourceType(DocumentType sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceNo() { return sourceNo; }
    public void setSourceNo(String sourceNo) { this.sourceNo = sourceNo; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public FundAccount getAccount() { return account; }
    public void setAccount(FundAccount account) { this.account = account; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
