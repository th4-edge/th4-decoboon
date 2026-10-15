package com.th4.edge.decoboon.project.model;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.model.BusinessStateView;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.configure.ProjectContractStage;
import com.th4.edge.decoboon.project.configure.ProjectRiskLevel;
import com.th4.edge.decoboon.project.configure.ProjectRiskType;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.field.annotation.LogicField;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 家装项目。
 *
 * <p>项目由已接受的报价生成，是施工、材料、验收和回款的经营单元。
 * 项目上的金额和进度属于派生汇总字段，由领域上下文统一刷新。</p>
 */
@Note("家装项目")
@Description("由客户报价转化而来的施工履约项目")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_project", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_company_property", columnNames = {"com_id", "property_id"})
}, indexes = {
        @Index(name = "idx_project_company_support", columnList = "com_id, support_id, project_id")
,
        @Index(name = "idx_project_company", columnList = "com_id, project_id"),
        @Index(name = "idx_project_company_store", columnList = "com_id, store_id, project_id"),
        @Index(name = "idx_project_company_team", columnList = "com_id, team_id, project_id"),
        @Index(name = "idx_project_company_owner", columnList = "com_id, owner_id, project_id"),
        @Index(name = "idx_project_company_designer_status", columnList = "com_id, designer_id, status, project_id")
})
public class Project extends AuditModel implements ProjectOwned {

    @Note("项目索引")
    @Id
    @SnowflakeId
    private Long projectId;

    @Note("业务来源")
    @Enumerated(EnumType.STRING)
    @Column(name = "business_origin", length = 16, nullable = false)
    private BusinessOrigin businessOrigin = BusinessOrigin.New;

    @Note("项目编号")
    @Keyword
    @Column(length = 64, nullable = false, unique = true)
    private String no;

    @Note("项目名称")
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

    @Note("经营负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Employee owner;


    @Note("客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("来源报价")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false, unique = true)
    private Quote quote;

    @Note("履约合同")
    @jakarta.persistence.OneToOne(mappedBy = "project", fetch = FetchType.LAZY)
    private ProjectContract contract;

    @Note("设计师")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    private Employee designer;

    @Note("项目经理")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Note("履约负责人（财务）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_id")
    private Employee support;

    @Note("工地地址")
    @Keyword
    @Column(length = 256, nullable = false)
    private String siteAddr;

    @Note("施工面积")
    @Unit("m²")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal area;

    @Note("合同金额")
    @Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal contractAmount = BigDecimal.ZERO;

    @Note("项目成本")
    @Unit("元")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal cost = BigDecimal.ZERO;

    @Note("项目状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private ProjectStatus status = ProjectStatus.Preparing;

    @Note("项目进度")
    @LogicField
    @Column(nullable = false)
    private Integer progress = 0;

    @Note("接管时项目进度基线；普通新项目为空")
    @Column(name = "baseline_progress")
    private Integer baselineProgress;

    @Note("计划开工日期")
    private LocalDate startDate;

    @Note("计划完工日期")
    private LocalDate planEndDate;

    @Note("预计完工日（动态，任务排期驱动；合同基准完工日见 ProjectContract.planEndDate）")
    private LocalDate expectedEndDate;

    @Note("实际完工日期")
    private LocalDate actualEndDate;

    @Transient
    @Note("当前施工阶段")
    private String currentStageName;

    @Transient
    @Note("只读投影：业主项目码 scene（= 项目编号，扫码进入参数）")
    private String ownerQrcodeScene;

    @Transient
    @Note("只读投影：业主项目码对象键（私有存储，不对外长期暴露）")
    private String ownerQrcodePath;

    @Transient
    @Note("只读投影：业主项目码短期签名地址（展示/打印用，过期需重新生成）")
    private String ownerQrcodeUrl;

    @Transient
    @Note("只读投影：业主项目码指向的版本环境（release/trial/develop）")
    private String ownerQrcodeEnvVersion;

    @Transient
    @Note("只读投影：项目风险等级")
    private ProjectRiskLevel riskLevel = ProjectRiskLevel.Normal;

    @Transient
    @Note("只读投影：命中经营预警条数")
    private Integer riskCount = 0;

    @Transient
    @Note("只读投影：主要风险类型")
    private ProjectRiskType mainRiskType = ProjectRiskType.None;

    @Transient
    @Note("只读投影：风险原因")
    private String riskReason;

    @Transient
    @Note("只读投影：下一步动作")
    private String nextAction;

    @Transient
    @Note("只读投影：服务端业务状态视图")
    private BusinessStateView businessStateView;

    @Transient
    @Unit("元")
    @Note("只读投影：已收工程款（来源于应收计划）")
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Transient
    @Unit("元")
    @Note("只读投影：应收工程款合计（来源于应收计划）")
    private BigDecimal receivableAmount = BigDecimal.ZERO;

    @Transient
    @Unit("元")
    @Note("只读投影：动态毛利；成本不完整时为 null，禁止用 0 冒充")
    private BigDecimal grossProfit;

    @Transient
    @Unit("%")
    @Note("只读投影：动态毛利率（百分比）；成本不完整时为 null，禁止用 0 冒充")
    private BigDecimal grossMargin;

    @Transient
    @Note("只读投影：应计成本是否完整")
    private Boolean costComplete = Boolean.TRUE;

    @Transient
    @Note("只读投影：计划完工逾期天数")
    private Integer overdueDays = 0;

    @Transient
    @Note("只读投影：逾期应收笔数")
    private Integer overdueReceivableCount = 0;

    @Transient
    @Unit("元")
    @Note("只读投影：实际成本（ProjectCostContext 口径）")
    private BigDecimal actualCost = BigDecimal.ZERO;

    @Transient
    @Note("只读投影：合同履约阶段")
    private ProjectContractStage contractStage = ProjectContractStage.Pending;

        public BusinessOrigin getBusinessOrigin() { return businessOrigin; }
    public void setBusinessOrigin(BusinessOrigin businessOrigin) { this.businessOrigin = businessOrigin; }
public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    public Employee getDesigner() {
        return designer;
    }

    public void setDesigner(Employee designer) {
        this.designer = designer;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    public Employee getSupport() {
        return support;
    }

    public void setSupport(Employee support) {
        this.support = support;
    }

    public String getSiteAddr() {
        return siteAddr;
    }

    public void setSiteAddr(String siteAddr) {
        this.siteAddr = siteAddr;
    }

    public BigDecimal getArea() {
        return area;
    }

    public void setArea(BigDecimal area) {
        this.area = area;
    }

    public BigDecimal getContractAmount() {
        return contractAmount;
    }

    public void setContractAmount(BigDecimal contractAmount) {
        this.contractAmount = contractAmount;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getBaselineProgress() {
        return baselineProgress;
    }

    public void setBaselineProgress(Integer baselineProgress) {
        this.baselineProgress = baselineProgress;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getPlanEndDate() {
        return planEndDate;
    }

    public void setPlanEndDate(LocalDate planEndDate) {
        this.planEndDate = planEndDate;
    }

    public LocalDate getExpectedEndDate() {
        return expectedEndDate;
    }

    public void setExpectedEndDate(LocalDate expectedEndDate) {
        this.expectedEndDate = expectedEndDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public String getCurrentStageName() { return currentStageName; }
    public void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }

    public ProjectRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ProjectRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public Integer getRiskCount() { return riskCount; }
    public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
    public ProjectRiskType getMainRiskType() { return mainRiskType; }
    public void setMainRiskType(ProjectRiskType mainRiskType) { this.mainRiskType = mainRiskType; }
    public String getRiskReason() { return riskReason; }
    public void setRiskReason(String riskReason) { this.riskReason = riskReason; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public BusinessStateView getBusinessStateView() { return businessStateView; }
    public void setBusinessStateView(BusinessStateView businessStateView) { this.businessStateView = businessStateView; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public BigDecimal getReceivableAmount() { return receivableAmount; }
    public void setReceivableAmount(BigDecimal receivableAmount) { this.receivableAmount = receivableAmount; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
    public BigDecimal getGrossMargin() { return grossMargin; }
    public void setGrossMargin(BigDecimal grossMargin) { this.grossMargin = grossMargin; }
    public Boolean getCostComplete() { return costComplete; }
    public void setCostComplete(Boolean costComplete) { this.costComplete = costComplete; }
    public Integer getOverdueDays() { return overdueDays; }
    public void setOverdueDays(Integer overdueDays) { this.overdueDays = overdueDays; }
    public Integer getOverdueReceivableCount() { return overdueReceivableCount; }
    public void setOverdueReceivableCount(Integer overdueReceivableCount) { this.overdueReceivableCount = overdueReceivableCount; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public ProjectContractStage getContractStage() { return contractStage; }
    public void setContractStage(ProjectContractStage contractStage) { this.contractStage = contractStage; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    @Override
    public Store getStore() { return store; }

    @Override
    public void setStore(Store store) { this.store = store; }

    @Override
    public Team getTeam() { return team; }

    @Override
    public void setTeam(Team team) { this.team = team; }

    @Override
    public Employee getOwner() { return owner; }

    @Override
    public void setOwner(Employee owner) { this.owner = owner; }

    public String getOwnerQrcodeScene() { return ownerQrcodeScene; }

    public void setOwnerQrcodeScene(String ownerQrcodeScene) { this.ownerQrcodeScene = ownerQrcodeScene; }

    public String getOwnerQrcodePath() { return ownerQrcodePath; }

    public void setOwnerQrcodePath(String ownerQrcodePath) { this.ownerQrcodePath = ownerQrcodePath; }

    public String getOwnerQrcodeUrl() { return ownerQrcodeUrl; }

    public void setOwnerQrcodeUrl(String ownerQrcodeUrl) { this.ownerQrcodeUrl = ownerQrcodeUrl; }

    public String getOwnerQrcodeEnvVersion() { return ownerQrcodeEnvVersion; }

    public void setOwnerQrcodeEnvVersion(String ownerQrcodeEnvVersion) { this.ownerQrcodeEnvVersion = ownerQrcodeEnvVersion; }

}
