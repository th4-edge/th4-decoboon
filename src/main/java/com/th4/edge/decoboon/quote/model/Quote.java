package com.th4.edge.decoboon.quote.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户报价快照。
 *
 * <p>报价可来源于设计方案的施工/材料分项，也可兼容原套餐报价。报价一旦生成，
 * 分项名称、规格、单价、金额和成本均为快照，后续主数据变化不追溯。</p>
 */
@Note("客户报价")
@Description("由设计方案分项或整装套餐生成的客户报价快照")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "createdBy")
@Table(name = "quote", indexes = {
        @Index(name = "idx_quote_company", columnList = "com_id, quote_id"),
        @Index(name = "idx_quote_company_store", columnList = "com_id, store_id, quote_id"),
        @Index(name = "idx_quote_company_team", columnList = "com_id, team_id, quote_id"),
        @Index(name = "idx_quote_company_owner", columnList = "com_id, emp_id, quote_id"),
        @Index(name = "idx_quote_company_design", columnList = "com_id, design_id, quote_id")
})
public class Quote extends AuditModel {

    @Note("报价索引")
    @Id
    @SnowflakeId
    private Long quoteId;

    @Note("报价编号")
    @Keyword
    @Column(length = 64, nullable = false, unique = true)
    private String no;

    @Note("报价名称")
    @Keyword
    @Column(length = 128)
    private String title;

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

    @Note("客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("设计方案")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id")
    private Design design;

    @Note("兼容套餐模板")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_template_id")
    private PackageTemplate packageTemplate;

    @Note("报价负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    private Employee createdBy;

    @Note("报价版本")
    @Column(nullable = false)
    private Integer version = 1;

    @Note("报价模式")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private QuoteMode mode = QuoteMode.Custom;

    @Note("计价面积")
    @Unit("m²")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal area = BigDecimal.ZERO;

    @Note("折前分项合计")
    @Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Note("折扣率")
    @Column(precision = 10, scale = 4, nullable = false)
    private BigDecimal discount = BigDecimal.ONE;

    @Note("税金")
    @Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Note("税率")
    @Column(precision = 8, scale = 4, nullable = false)
    private BigDecimal taxRate = new BigDecimal("0.05");

    @Note("报价金额")
    @Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("报价成本")
    @Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal cost = BigDecimal.ZERO;

    @Note("成本是否完整")
    @Column(nullable = false)
    private Boolean costComplete = Boolean.FALSE;

    @Note("设计费")
    @Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal designFee = BigDecimal.ZERO;

    @Note("管理费")
    @Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal managementFee = BigDecimal.ZERO;

    @Note("报价状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private QuoteStatus status = QuoteStatus.Draft;

    @Note("首次提交时间")
    private LocalDateTime submittedAt;

    @Note("客户驳回时间")
    private LocalDateTime rejectedAt;

    @Note("客户接受时间")
    private LocalDateTime acceptedAt;

    @Note("转项目时间")
    private LocalDateTime convertedAt;

    @Note("报价明细")
    @Transient
    private List<QuoteLine> items;

    @Note("按区域汇总")
    @Transient
    private List<QuoteGroup> regionGroups;

    @Note("按施工分项汇总")
    @Transient
    private List<QuoteGroup> constructionGroups;

    @Note("按材料分项汇总")
    @Transient
    private List<QuoteGroup> materialGroups;

    @Note("按软装、定制、电器、综合分项汇总")
    @Transient
    private List<QuoteGroup> otherGroups;

    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Design getDesign() { return design; }
    public void setDesign(Design design) { this.design = design; }
    public PackageTemplate getPackageTemplate() { return packageTemplate; }
    public void setPackageTemplate(PackageTemplate packageTemplate) { this.packageTemplate = packageTemplate; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public QuoteMode getMode() { return mode; }
    public void setMode(QuoteMode mode) { this.mode = mode; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public BigDecimal getDesignFee() { return designFee; }
    public void setDesignFee(BigDecimal designFee) { this.designFee = designFee; }
    public BigDecimal getManagementFee() { return managementFee; }
    public void setManagementFee(BigDecimal managementFee) { this.managementFee = managementFee; }
    public QuoteStatus getStatus() { return status; }
    public void setStatus(QuoteStatus status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getConvertedAt() { return convertedAt; }
    public void setConvertedAt(LocalDateTime convertedAt) { this.convertedAt = convertedAt; }
    public List<QuoteLine> getItems() { return items; }
    public void setItems(List<QuoteLine> items) { this.items = items; }
    public List<QuoteGroup> getRegionGroups() { return regionGroups; }
    public void setRegionGroups(List<QuoteGroup> regionGroups) { this.regionGroups = regionGroups; }
    public List<QuoteGroup> getConstructionGroups() { return constructionGroups; }
    public void setConstructionGroups(List<QuoteGroup> constructionGroups) { this.constructionGroups = constructionGroups; }
    public List<QuoteGroup> getMaterialGroups() { return materialGroups; }
    public void setMaterialGroups(List<QuoteGroup> materialGroups) { this.materialGroups = materialGroups; }
    public List<QuoteGroup> getOtherGroups() { return otherGroups; }
    public void setOtherGroups(List<QuoteGroup> otherGroups) { this.otherGroups = otherGroups; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
}
