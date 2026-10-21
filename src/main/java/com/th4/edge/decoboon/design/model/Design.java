package com.th4.edge.decoboon.design.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.field.annotation.LogicField;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.math.BigDecimal;
import java.util.List;

/** 面向单个客户和房屋的一套设计方案。 */
@Note("设计方案")
@Description("量房后形成的户型、区域尺寸、CAD、设计表达和分项清单主档")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_design", indexes = {
        @Index(name = "idx_design_company", columnList = "com_id, design_id"),
        @Index(name = "idx_design_company_store", columnList = "com_id, store_id, design_id"),
        @Index(name = "idx_design_company_team", columnList = "com_id, team_id, design_id"),
        @Index(name = "idx_design_company_owner", columnList = "com_id, emp_id, design_id"),
        @Index(name = "idx_design_company_customer", columnList = "com_id, customer_id, design_id"),
        @Index(name = "idx_design_company_property_status", columnList = "com_id, property_id, status, design_id")
})
public class Design extends AuditModel {

    @Note("方案索引")
    @Id
    @SnowflakeId
    private Long designId;

    @Note("方案编号")
    @Keyword
    @Column(length = 64, nullable = false, unique = true)
    private String no;

    @Note("方案名称")
    @Keyword
    @Column(length = 128, nullable = false)
    private String name;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("实例化来源：设计模版版本（同房屋同版本唯一，用于幂等）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_template_version_id")
    private DesignTemplateVersion designTemplateVersion;

    @Note("方案版本")
    @Column(nullable = false)
    private Integer version = 1;

    @Note("设计风格")
    @Keyword
    @Column(length = 64)
    private String style;

    @Note("方案状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private DesignStatus status = DesignStatus.Draft;

    @Note("套内面积")
    @Unit("m²")
    @LogicField
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal area = BigDecimal.ZERO;

    @Note("户型摘要")
    @Column(length = 256)
    private String layout;

    @Note("方案说明")
    @Lob
    private String description;

    @Note("封面图片")
    @Column(length = 512)
    private String coverUrl;

    @Note("CAD 图纸")
    @Column(length = 512)
    private String cadUrl;

    @Note("CAD 预览图")
    @Column(length = 512)
    private String cadPreviewUrl;

    @Note("CAD 转换状态")
    @Column(length = 16)
    private String cadConvertStatus;

    @Note("原始结构图")
    @Column(length = 512)
    private String originalPlanUrl;

    @Note("拆改图")
    @Column(length = 512)
    private String demolitionUrl;

    @Note("砌筑图")
    @Column(length = 512)
    private String masonryUrl;

    @Note("优化图")
    @Column(length = 512)
    private String optimizationUrl;

    @Note("原始结构图（多图）")
    @ElementCollection
    @CollectionTable(name = "db_design_original_plan_images", joinColumns = @JoinColumn(name = "design_id"))
    @Column(name = "url", length = 512)
    private List<String> originalPlanImages;

    @Note("现场照片")
    @ElementCollection
    @CollectionTable(name = "db_design_site_photos", joinColumns = @JoinColumn(name = "design_id"))
    @Column(name = "url", length = 512)
    private List<String> sitePhotos;

    @Note("量房照片")
    @ElementCollection
    @CollectionTable(name = "db_design_survey_photos", joinColumns = @JoinColumn(name = "design_id"))
    @Column(name = "url", length = 512)
    private List<String> surveyPhotos;

    @Transient
    @Note("方案区域")
    private List<Space> spaces;

    @Transient
    @Note("设计分项")
    private List<DesignItem> items;

    @Transient
    @Note("客户诉求")
    private List<Requirement> requirements;

    @Transient
    @Note("方案计算价（当前设计分项销售金额合计）")
    private BigDecimal calculatedAmount = BigDecimal.ZERO;

    @Transient
    @Note("方案区域数")
    private Integer spaceCount = 0;

    @Transient
    @Note("设计分项数")
    private Integer itemCount = 0;

    @Transient
    @Note("当前正式报价索引")
    private Long currentQuoteId;

    @Transient
    @Note("当前正式报价编号")
    private String currentQuoteNo;

    @Transient
    @Note("当前正式报价版本")
    private Integer currentQuoteVersion;

    @Transient
    @Note("当前正式报价状态")
    private QuoteStatus currentQuoteStatus;

    @Transient
    @Note("当前正式报价状态名称")
    private String currentQuoteStatusName;

    @Transient
    @Note("当前正式报价金额")
    private BigDecimal currentQuoteAmount;

    public Long getDesignId() { return designId; }
    public void setDesignId(Long designId) { this.designId = designId; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public DesignStatus getStatus() { return status; }
    public void setStatus(DesignStatus status) { this.status = status; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public String getLayout() { return layout; }
    public void setLayout(String layout) { this.layout = layout; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getCadUrl() { return cadUrl; }
    public void setCadUrl(String cadUrl) { this.cadUrl = cadUrl; }
    public String getCadPreviewUrl() { return cadPreviewUrl; }
    public void setCadPreviewUrl(String cadPreviewUrl) { this.cadPreviewUrl = cadPreviewUrl; }
    public String getCadConvertStatus() { return cadConvertStatus; }
    public void setCadConvertStatus(String cadConvertStatus) { this.cadConvertStatus = cadConvertStatus; }
    public String getOriginalPlanUrl() { return originalPlanUrl; }
    public void setOriginalPlanUrl(String originalPlanUrl) { this.originalPlanUrl = originalPlanUrl; }
    public String getDemolitionUrl() { return demolitionUrl; }
    public void setDemolitionUrl(String demolitionUrl) { this.demolitionUrl = demolitionUrl; }
    public String getMasonryUrl() { return masonryUrl; }
    public void setMasonryUrl(String masonryUrl) { this.masonryUrl = masonryUrl; }
    public String getOptimizationUrl() { return optimizationUrl; }
    public void setOptimizationUrl(String optimizationUrl) { this.optimizationUrl = optimizationUrl; }
    public List<String> getOriginalPlanImages() { return originalPlanImages; }
    public void setOriginalPlanImages(List<String> originalPlanImages) { this.originalPlanImages = originalPlanImages; }

    public List<String> getSitePhotos() { return sitePhotos; }
    public void setSitePhotos(List<String> sitePhotos) { this.sitePhotos = sitePhotos; }
    public List<String> getSurveyPhotos() { return surveyPhotos; }
    public void setSurveyPhotos(List<String> surveyPhotos) { this.surveyPhotos = surveyPhotos; }
    public List<Space> getSpaces() { return spaces; }
    public void setSpaces(List<Space> spaces) { this.spaces = spaces; }
    public List<DesignItem> getItems() { return items; }
    public void setItems(List<DesignItem> items) { this.items = items; }
    public List<Requirement> getRequirements() { return requirements; }
    public void setRequirements(List<Requirement> requirements) { this.requirements = requirements; }
    public BigDecimal getCalculatedAmount() { return calculatedAmount; }
    public void setCalculatedAmount(BigDecimal calculatedAmount) { this.calculatedAmount = calculatedAmount; }
    public Integer getSpaceCount() { return spaceCount; }
    public void setSpaceCount(Integer spaceCount) { this.spaceCount = spaceCount; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Long getCurrentQuoteId() { return currentQuoteId; }
    public void setCurrentQuoteId(Long currentQuoteId) { this.currentQuoteId = currentQuoteId; }
    public String getCurrentQuoteNo() { return currentQuoteNo; }
    public void setCurrentQuoteNo(String currentQuoteNo) { this.currentQuoteNo = currentQuoteNo; }
    public Integer getCurrentQuoteVersion() { return currentQuoteVersion; }
    public void setCurrentQuoteVersion(Integer currentQuoteVersion) { this.currentQuoteVersion = currentQuoteVersion; }
    public QuoteStatus getCurrentQuoteStatus() { return currentQuoteStatus; }
    public void setCurrentQuoteStatus(QuoteStatus currentQuoteStatus) { this.currentQuoteStatus = currentQuoteStatus; }
    public String getCurrentQuoteStatusName() { return currentQuoteStatusName; }
    public void setCurrentQuoteStatusName(String currentQuoteStatusName) { this.currentQuoteStatusName = currentQuoteStatusName; }
    public BigDecimal getCurrentQuoteAmount() { return currentQuoteAmount; }
    public void setCurrentQuoteAmount(BigDecimal currentQuoteAmount) { this.currentQuoteAmount = currentQuoteAmount; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public DesignTemplateVersion getDesignTemplateVersion() { return designTemplateVersion; }

    public void setDesignTemplateVersion(DesignTemplateVersion designTemplateVersion) {
        this.designTemplateVersion = designTemplateVersion;
    }
}
