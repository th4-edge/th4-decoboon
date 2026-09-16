package com.th4.edge.decoboon.design.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.TalkStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.List;

/** 面向到店谈单场景自动组织的 PPT 内容。 */
@Note("谈单PPT")
@Description("把客户诉求、量房数据、设计方案和报价组合成有说服力的谈单页结构")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_design_talk", indexes = {
        @Index(name = "idx_talk_company", columnList = "com_id, talk_id"),
        @Index(name = "idx_talk_company_store", columnList = "com_id, store_id, talk_id"),
        @Index(name = "idx_talk_company_team", columnList = "com_id, team_id, talk_id"),
        @Index(name = "idx_talk_company_owner", columnList = "com_id, emp_id, talk_id"),
        @Index(name = "idx_talk_company_customer", columnList = "com_id, customer_id, talk_id")
})
public class Talk extends AuditModel {

    @Note("谈单索引")
    @Id
    @SnowflakeId
    private Long talkId;

    @Note("谈单编号")
    @Keyword
    @Column(length = 64, nullable = false, unique = true)
    private String no;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("负责设计师")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee owner;

    @Note("客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Note("主推方案")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("关联报价")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @Note("谈单标题")
    @Keyword
    @Column(length = 128, nullable = false)
    private String title;

    @Note("版本")
    @Column(nullable = false)
    private Integer version = 1;

    @Note("状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private TalkStatus status = TalkStatus.Draft;

    @Note("封面图片")
    @Column(length = 512)
    private String coverUrl;

    @Note("导出文件地址")
    @Column(length = 512)
    private String outputUrl;

    @Note("谈单说明")
    @Lob
    private String description;

    @Note("生成时间")
    private LocalDateTime builtAt;

    @Transient
    @Note("谈单页")
    private List<TalkSection> sections;

    public Long getTalkId() { return talkId; }
    public void setTalkId(Long talkId) { this.talkId = talkId; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Design getDesign() { return design; }
    public void setDesign(Design design) { this.design = design; }
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public TalkStatus getStatus() { return status; }
    public void setStatus(TalkStatus status) { this.status = status; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getOutputUrl() { return outputUrl; }
    public void setOutputUrl(String outputUrl) { this.outputUrl = outputUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getBuiltAt() { return builtAt; }
    public void setBuiltAt(LocalDateTime builtAt) { this.builtAt = builtAt; }
    public List<TalkSection> getSections() { return sections; }
    public void setSections(List<TalkSection> sections) { this.sections = sections; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
}
