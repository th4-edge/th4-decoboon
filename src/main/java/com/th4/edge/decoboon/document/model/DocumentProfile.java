package com.th4.edge.decoboon.document.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

/** 企业单据抬头资料。 */
@Note("企业单据资料")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_document_profile", uniqueConstraints = @UniqueConstraint(
        name = "uk_document_profile_company", columnNames = "com_id"))
public class DocumentProfile extends AuditModel {

    @Id @SnowflakeId @Note("单据资料索引") private Long profileId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @Keyword @Column(length = 128) @Note("企业法定名称") private String legalName;
    @Keyword @Column(length = 32) @Note("统一社会信用代码") private String creditCode;
    @Column(length = 32) @Note("法定代表人") private String legalRepresentative;
    @Column(length = 256) @Note("通讯地址") private String address;
    @Column(length = 32) @Note("服务热线") private String serviceHotline;
    @Column(length = 64) @Note("开户行") private String bankName;
    @Column(length = 64) @Note("银行账号") private String bankAccount;
    @Column(length = 512) @Note("企业Logo") private String logo;

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getCreditCode() { return creditCode; }
    public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
    public String getLegalRepresentative() { return legalRepresentative; }
    public void setLegalRepresentative(String legalRepresentative) { this.legalRepresentative = legalRepresentative; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getServiceHotline() { return serviceHotline; }
    public void setServiceHotline(String serviceHotline) { this.serviceHotline = serviceHotline; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
}
