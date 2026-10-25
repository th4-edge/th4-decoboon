package com.th4.edge.decoboon.customer.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.configure.CustomerDormantReason;
import com.th4.edge.decoboon.customer.configure.CustomerLossReason;
import com.th4.edge.decoboon.customer.configure.CustomerLossScope;
import com.th4.edge.decoboon.customer.configure.CustomerLossStage;
import com.th4.edge.decoboon.customer.configure.CustomerRiskLevel;
import com.th4.edge.decoboon.customer.configure.Gender;
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
 * 客户实体。
 *
 * <p>客户属于企业租户，可由员工负责跟进。项目通过 {@code Project.customer}
 * 反向关联，支持同一客户多套房、多次复购。</p>
 */
@Note("客户")
@Description("家装企业正式维护的装修客户")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "cust_customer", indexes = {
        @Index(name = "idx_cust_customer_company", columnList = "com_id, cust_id"),
        @Index(name = "idx_cust_customer_company_store", columnList = "com_id, store_id, cust_id"),
        @Index(name = "idx_cust_customer_company_team", columnList = "com_id, team_id, cust_id"),
        @Index(name = "idx_cust_customer_company_owner", columnList = "com_id, emp_id, cust_id"),
        @Index(name = "idx_cust_customer_company_designer", columnList = "com_id, designer_id, cust_id")
})
public class Customer extends AuditModel {

    @Note("客户索引")
    @Id
    @SnowflakeId
    private Long custId;

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

    @Note("客户姓名")
    @Keyword
    @Column(length = 32, nullable = false)
    private String name;

    @Note("联系电话")
    @Keyword
    @Column(length = 20, nullable = false)
    private String telNo;

    @Note("客户性别")
    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Gender gender = Gender.M;

    @Note("客户来源")
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private CustomerSource source = CustomerSource.Other;

    @Note("意向小区")
    @Keyword
    @Column(length = 128)
    private String community;

    @Note("房屋地址")
    @Keyword
    @Column(length = 256)
    private String addr;

    @Note("房屋面积")
    @Unit("m²")
    @Column(precision = 10, scale = 2)
    private BigDecimal area;

    @Note("装修预算（万元）")
    @Unit("万元")
    @Column(precision = 12, scale = 2)
    private BigDecimal budget;

    @Note("客户状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private CustStatus status = CustStatus.New;

    @Note("负责员工")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    private Employee owner;

    @Note("接手设计师")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    private Employee designer;

    @Note("客户备注")
    @Column(length = 512)
    private String remark;

    @Note("最近跟进时间")
    private LocalDateTime lastFollowAt;

    @Note("下次跟进时间")
    private LocalDateTime nextFollowAt;

    @Note("休眠前客户状态")
    @Enumerated(EnumType.STRING)
    @Column(name = "status_before_dormant", length = 16)
    private CustStatus statusBeforeDormant;

    @Note("休眠原因")
    @Enumerated(EnumType.STRING)
    @Column(name = "dormant_reason", length = 32)
    private CustomerDormantReason dormantReason;

    @Note("休眠原因说明")
    @Column(name = "dormant_reason_text", length = 200)
    private String dormantReasonText;

    @Note("休眠时间")
    @Column(name = "dormant_at")
    private LocalDateTime dormantAt;

    @Note("休眠操作人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dormant_by")
    private Employee dormantBy;

    @Note("最近恢复时间")
    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    @Note("最近恢复操作人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restored_by")
    private Employee restoredBy;

    @Note("是否已加微信")
    @Column(nullable = false)
    private Boolean wechatAdded = Boolean.FALSE;

    @Note("微信号")
    @Column(length = 64)
    private String wechatNo;

    @Note("添加微信时间")
    private LocalDateTime wechatAddedAt;

    @Note("到店次数")
    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer visitCount = 0;

    @Note("最近到店时间")
    private LocalDateTime lastVisitAt;

    @Note("进入待量房时间")
    private LocalDateTime pendingMeasureAt;

    @Note("约定量房时间")
    private LocalDateTime measureAt;

    @Note("记录量房约定时间")
    private LocalDateTime measureRequestedAt;

    @Note("指定设计师时间")
    private LocalDateTime handedOffAt;

    // ==== 以下为只读投影字段，不落库，由 Provider 按需装配（P0-b） ====

    @Transient
    @Note("标签（系统标签 + 人工标签）")
    private List<CustomerTag> tags;

    @Transient
    @Note("客户画像")
    private CustomerPortrait portrait;

    @Transient
    @Note("客户分级（派生值，只读）")
    private CustomerGrade grade;

    @Transient
    @Note("只读投影：停留天数")
    private Integer idleDays = 0;

    @Transient
    @Note("只读投影：客户风险等级")
    private CustomerRiskLevel riskLevel = CustomerRiskLevel.Normal;

    @Transient
    @Note("只读投影：风险原因")
    private String riskReason;

    @Transient
    @Note("只读投影：下一步动作（风险驱动，兜底动作）")
    private String nextAction;

    @Transient
    @Note("只读投影：主链主行动编码")
    private String mainlineKey;

    @Transient
    @Note("只读投影：主链主行动")
    private String mainlineLabel;

    @Transient
    @Note("只读投影：主链主行动说明")
    private String mainlineHint;

    @Transient
    @Unit("元")
    @Note("只读投影：最近报价金额")
    private BigDecimal quoteAmount;

    @Transient
    @Unit("元")
    @Note("只读投影：合同签约金额")
    private BigDecimal signedAmount;

    @Transient
    @Note("只读投影：项目名称")
    private String projectName;

    @Transient
    @Note("只读投影：项目索引")
    private Long projectId;

    // ==== 以下为当前流失摘要只读投影，不落库；历史记录见 CustomerLoss ====

    @Transient
    @Note("当前流失记录索引")
    private Long lossId;

    @Transient
    @Note("当前流失范围")
    private CustomerLossScope lossScope;

    @Transient
    @Note("当前流失阶段")
    private CustomerLossStage lossStage;

    @Transient
    @Note("当前流失原因")
    private CustomerLossReason lossReason;

    @Transient
    @Note("当前流失原因说明")
    private String lossReasonText;

    @Transient
    @Note("竞争对手名称")
    private String competitorName;

    @Transient
    @Note("流失时间")
    private LocalDateTime lostAt;

    @Transient
    @Note("流失操作人")
    private Employee lostBy;

    @Transient
    @Note("流失房屋索引")
    private Long lossPropertyId;

    @Transient
    @Note("流失房屋摘要")
    private String lossPropertyName;

    @Transient
    @Note("流失前状态")
    private CustStatus lossFromStatus;

    @Transient
    @Note("当前活跃流失记录数")
    private Integer activeLossCount;

    @Transient
    @Note("是否为部分房屋流失")
    private Boolean partialLoss;

    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public CustomerSource getSource() { return source; }
    public void setSource(CustomerSource source) { this.source = source; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public CustStatus getStatus() { return status; }
    public void setStatus(CustStatus status) { this.status = status; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Employee getDesigner() { return designer; }
    public void setDesigner(Employee designer) { this.designer = designer; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getLastFollowAt() { return lastFollowAt; }
    public void setLastFollowAt(LocalDateTime lastFollowAt) { this.lastFollowAt = lastFollowAt; }
    public LocalDateTime getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(LocalDateTime nextFollowAt) { this.nextFollowAt = nextFollowAt; }
    public CustStatus getStatusBeforeDormant() { return statusBeforeDormant; }
    public void setStatusBeforeDormant(CustStatus statusBeforeDormant) { this.statusBeforeDormant = statusBeforeDormant; }
    public CustomerDormantReason getDormantReason() { return dormantReason; }
    public void setDormantReason(CustomerDormantReason dormantReason) { this.dormantReason = dormantReason; }
    public String getDormantReasonText() { return dormantReasonText; }
    public void setDormantReasonText(String dormantReasonText) { this.dormantReasonText = dormantReasonText; }
    public LocalDateTime getDormantAt() { return dormantAt; }
    public void setDormantAt(LocalDateTime dormantAt) { this.dormantAt = dormantAt; }
    public Employee getDormantBy() { return dormantBy; }
    public void setDormantBy(Employee dormantBy) { this.dormantBy = dormantBy; }
    public LocalDateTime getRestoredAt() { return restoredAt; }
    public void setRestoredAt(LocalDateTime restoredAt) { this.restoredAt = restoredAt; }
    public Employee getRestoredBy() { return restoredBy; }
    public void setRestoredBy(Employee restoredBy) { this.restoredBy = restoredBy; }
    public Boolean getWechatAdded() { return wechatAdded; }
    public void setWechatAdded(Boolean wechatAdded) { this.wechatAdded = wechatAdded; }
    public String getWechatNo() { return wechatNo; }
    public void setWechatNo(String wechatNo) { this.wechatNo = wechatNo; }
    public LocalDateTime getWechatAddedAt() { return wechatAddedAt; }
    public void setWechatAddedAt(LocalDateTime wechatAddedAt) { this.wechatAddedAt = wechatAddedAt; }
    public Integer getVisitCount() { return visitCount; }
    public void setVisitCount(Integer visitCount) { this.visitCount = visitCount; }
    public LocalDateTime getLastVisitAt() { return lastVisitAt; }
    public void setLastVisitAt(LocalDateTime lastVisitAt) { this.lastVisitAt = lastVisitAt; }
    public LocalDateTime getPendingMeasureAt() { return pendingMeasureAt; }
    public void setPendingMeasureAt(LocalDateTime pendingMeasureAt) { this.pendingMeasureAt = pendingMeasureAt; }
    public LocalDateTime getMeasureAt() { return measureAt; }
    public void setMeasureAt(LocalDateTime measureAt) { this.measureAt = measureAt; }
    public LocalDateTime getMeasureRequestedAt() { return measureRequestedAt; }
    public void setMeasureRequestedAt(LocalDateTime measureRequestedAt) { this.measureRequestedAt = measureRequestedAt; }
    public LocalDateTime getHandedOffAt() { return handedOffAt; }
    public void setHandedOffAt(LocalDateTime handedOffAt) { this.handedOffAt = handedOffAt; }

    public List<CustomerTag> getTags() { return tags; }
    public void setTags(List<CustomerTag> tags) { this.tags = tags; }
    public CustomerPortrait getPortrait() { return portrait; }
    public void setPortrait(CustomerPortrait portrait) { this.portrait = portrait; }
    public CustomerGrade getGrade() { return grade; }
    public void setGrade(CustomerGrade grade) { this.grade = grade; }
    public Integer getIdleDays() { return idleDays; }
    public void setIdleDays(Integer idleDays) { this.idleDays = idleDays; }
    public CustomerRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(CustomerRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }

    public String getMainlineKey() { return mainlineKey; }
    public void setMainlineKey(String mainlineKey) { this.mainlineKey = mainlineKey; }
    public String getMainlineLabel() { return mainlineLabel; }
    public void setMainlineLabel(String mainlineLabel) { this.mainlineLabel = mainlineLabel; }
    public String getMainlineHint() { return mainlineHint; }
    public void setMainlineHint(String mainlineHint) { this.mainlineHint = mainlineHint; }
    public BigDecimal getQuoteAmount() { return quoteAmount; }
    public void setQuoteAmount(BigDecimal quoteAmount) { this.quoteAmount = quoteAmount; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getLossId() { return lossId; }
    public void setLossId(Long lossId) { this.lossId = lossId; }
    public CustomerLossScope getLossScope() { return lossScope; }
    public void setLossScope(CustomerLossScope lossScope) { this.lossScope = lossScope; }
    public CustomerLossStage getLossStage() { return lossStage; }
    public void setLossStage(CustomerLossStage lossStage) { this.lossStage = lossStage; }
    public CustomerLossReason getLossReason() { return lossReason; }
    public void setLossReason(CustomerLossReason lossReason) { this.lossReason = lossReason; }
    public String getLossReasonText() { return lossReasonText; }
    public void setLossReasonText(String lossReasonText) { this.lossReasonText = lossReasonText; }
    public String getCompetitorName() { return competitorName; }
    public void setCompetitorName(String competitorName) { this.competitorName = competitorName; }
    public LocalDateTime getLostAt() { return lostAt; }
    public void setLostAt(LocalDateTime lostAt) { this.lostAt = lostAt; }
    public Employee getLostBy() { return lostBy; }
    public void setLostBy(Employee lostBy) { this.lostBy = lostBy; }
    public Long getLossPropertyId() { return lossPropertyId; }
    public void setLossPropertyId(Long lossPropertyId) { this.lossPropertyId = lossPropertyId; }
    public String getLossPropertyName() { return lossPropertyName; }
    public void setLossPropertyName(String lossPropertyName) { this.lossPropertyName = lossPropertyName; }
    public CustStatus getLossFromStatus() { return lossFromStatus; }
    public void setLossFromStatus(CustStatus lossFromStatus) { this.lossFromStatus = lossFromStatus; }
    public Integer getActiveLossCount() { return activeLossCount; }
    public void setActiveLossCount(Integer activeLossCount) { this.activeLossCount = activeLossCount; }
    public Boolean getPartialLoss() { return partialLoss; }
    public void setPartialLoss(Boolean partialLoss) { this.partialLoss = partialLoss; }
}
