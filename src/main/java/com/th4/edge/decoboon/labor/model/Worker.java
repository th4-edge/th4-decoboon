package com.th4.edge.decoboon.labor.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.labor.configure.WorkerStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
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

/**
 * 个人工人档案。
 *
 * <p>企业级共享主数据：同一工人在企业内只建一份档案，跨项目复用，
 * 作为个人工资发放凭证的对方来源。身份证号与银行账号属敏感字段，
 * 由 {@code WorkerPrivacyContext} 按角色裁剪。</p>
 */
@Note("工人档案")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_worker", indexes = {
        @Index(name = "idx_worker_company", columnList = "com_id, worker_id"),
        @Index(name = "idx_worker_company_id_card", columnList = "com_id, id_card_no")
})
public class Worker extends AuditModel {

    @Id @SnowflakeId @Note("工人索引")
    private Long workerId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业")
    private Company company;

    @Keyword @Column(length = 64, nullable = false) @Note("姓名")
    private String name;

    @Column(name = "id_card_no", length = 32) @Note("身份证号码")
    private String idCardNo;

    @Keyword @Column(length = 32) @Note("工种")
    private String trade;

    @Keyword @Column(length = 20) @Note("手机号码")
    private String phone;

    @Column(length = 64) @Note("开户行")
    private String bankName;

    @Column(length = 64) @Note("银行账号")
    private String bankAccount;

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("状态")
    private WorkerStatus status = WorkerStatus.Active;

    @Column(length = 500) @Note("备注")
    private String remark;

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdCardNo() { return idCardNo; }
    public void setIdCardNo(String idCardNo) { this.idCardNo = idCardNo; }
    public String getTrade() { return trade; }
    public void setTrade(String trade) { this.trade = trade; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public WorkerStatus getStatus() { return status; }
    public void setStatus(WorkerStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
