package com.th4.edge.decoboon.design.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.RequirementCategory;
import com.th4.edge.decoboon.design.configure.RequirementKind;
import com.th4.edge.decoboon.design.configure.RequirementPriority;
import com.th4.edge.decoboon.design.configure.RequirementStatus;
import com.th4.edge.decoboon.bim.configure.SpaceType;
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

import java.time.LocalDateTime;

/** 设计师与客户沟通中沉淀的诉求点或排斥点。 */
@Note("客户装修诉求")
@Description("记录客户明确要求、偏好、排斥项和设计回应，作为方案与谈单依据")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_design_requirement", indexes = {
        @Index(name = "idx_requirement_company", columnList = "com_id, requirement_id"),
        @Index(name = "idx_requirement_company_store", columnList = "com_id, store_id, requirement_id"),
        @Index(name = "idx_requirement_company_team", columnList = "com_id, team_id, requirement_id"),
        @Index(name = "idx_requirement_company_owner", columnList = "com_id, emp_id, requirement_id"),
        @Index(name = "idx_requirement_company_customer", columnList = "com_id, customer_id, requirement_id")
})
public class Requirement extends AuditModel {

    @Note("诉求索引")
    @Id
    @SnowflakeId
    private Long requirementId;

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
    @JoinColumn(name = "emp_id")
    private Employee owner;

    @Note("客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @Note("关联空间类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private SpaceType type;

    @Note("区域名称")
    @Keyword
    @Column(length = 64)
    private String spaceName;

    @Note("诉求类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private RequirementKind kind = RequirementKind.Demand;

    @Note("诉求分类")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private RequirementCategory category = RequirementCategory.Other;

    @Note("优先级")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private RequirementPriority priority = RequirementPriority.Medium;

    @Note("落地状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private RequirementStatus status = RequirementStatus.Pending;

    @Note("诉求标题")
    @Keyword
    @Column(length = 128, nullable = false)
    private String title;

    @Note("原始描述")
    @Lob
    private String detail;

    @Note("设计回应")
    @Lob
    private String response;

    @Note("信息来源")
    @Column(length = 32)
    private String source;

    @Note("确认时间")
    private LocalDateTime confirmedAt;

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
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
    public SpaceType getType() { return type; }
    public void setType(SpaceType type) { this.type = type; }
    public String getSpaceName() { return spaceName; }
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }
    public RequirementKind getKind() { return kind; }
    public void setKind(RequirementKind kind) { this.kind = kind; }
    public RequirementCategory getCategory() { return category; }
    public void setCategory(RequirementCategory category) { this.category = category; }
    public RequirementPriority getPriority() { return priority; }
    public void setPriority(RequirementPriority priority) { this.priority = priority; }
    public RequirementStatus getStatus() { return status; }
    public void setStatus(RequirementStatus status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
}
