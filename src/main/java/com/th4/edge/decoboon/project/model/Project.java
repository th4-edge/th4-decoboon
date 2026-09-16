package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
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
@Table(name = "db_decoboon_project", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_company_property", columnNames = {"com_id", "property_id"})
}, indexes = {
        @Index(name = "idx_project_company_support", columnList = "com_id, support_id, project_id")
,
        @Index(name = "idx_project_company", columnList = "com_id, project_id"),
        @Index(name = "idx_project_company_store", columnList = "com_id, store_id, project_id"),
        @Index(name = "idx_project_company_team", columnList = "com_id, team_id, project_id"),
        @Index(name = "idx_project_company_owner", columnList = "com_id, owner_id, project_id")
})
public class Project extends AuditModel implements ProjectOwned {

    @Note("项目索引")
    @Id
    @SnowflakeId
    private Long projectId;

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

    @Note("履约专员")
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

    @Note("计划开工日期")
    private LocalDate startDate;

    @Note("计划完工日期")
    private LocalDate planEndDate;

    @Note("实际完工日期")
    private LocalDate actualEndDate;

    @Transient
    @Note("当前施工阶段")
    private String currentStageName;

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

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public String getCurrentStageName() { return currentStageName; }
    public void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }

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


}
