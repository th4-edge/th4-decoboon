package com.th4.edge.decoboon.customer.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackKind;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackType;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectOwned;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.time.LocalDateTime;
import java.util.List;

/** 业主提交的意见或投诉事项。 */
@Note("业主意见投诉")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "cust_customer_feedback", indexes = {
        @Index(name = "idx_customer_feedback_company", columnList = "com_id, feedback_id"),
        @Index(name = "idx_customer_feedback_project", columnList = "com_id, project_id, feedback_id"),
        @Index(name = "idx_customer_feedback_status", columnList = "com_id, status, feedback_id"),
        @Index(name = "idx_customer_feedback_credential", columnList = "com_id, cre_id, feedback_id"),
        @Index(name = "idx_customer_feedback_store", columnList = "com_id, store_id, feedback_id"),
        @Index(name = "idx_customer_feedback_team", columnList = "com_id, team_id, feedback_id"),
        @Index(name = "idx_customer_feedback_owner", columnList = "com_id, owner_id, feedback_id")
})
public class CustomerFeedback extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("反馈索引")
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("所属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Note("所属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Note("经营负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Note("所属项目")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    @Note("关联阶段（可为空）")
    private ProjectStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_id", nullable = false)
    @Note("关联客户")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cre_id", nullable = false)
    @Note("提交业主身份")
    private Credentials credential;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("反馈性质")
    private CustomerFeedbackKind kind = CustomerFeedbackKind.Opinion;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("反馈类型")
    private CustomerFeedbackType type = CustomerFeedbackType.Other;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("处理状态")
    private CustomerFeedbackStatus status = CustomerFeedbackStatus.Submitted;

    @Column(length = 128, nullable = false)
    @Note("标题")
    private String title;

    @Column(length = 2048, nullable = false)
    @Note("问题或诉求")
    private String content;

    @ElementCollection
    @CollectionTable(name = "cust_customer_feedback_image", joinColumns = @JoinColumn(name = "feedback_id"))
    @Column(name = "url", length = 512)
    @Note("现场图片")
    private List<String> images;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id")
    @Note("当前处理人")
    private Employee handler;

    @Column(length = 2048)
    @Note("处理回复")
    private String response;

    @Note("首次响应时间")
    private LocalDateTime respondedAt;

    @Note("预计解决时间")
    private LocalDateTime dueAt;

    @Note("关闭时间")
    private LocalDateTime closedAt;

    @Note("业主满意度（1-5）")
    private Integer satisfaction;

    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    @Override public Company getCompany() { return company; }
    @Override public void setCompany(Company company) { this.company = company; }
    @Override public Store getStore() { return store; }
    @Override public void setStore(Store store) { this.store = store; }
    @Override public Team getTeam() { return team; }
    @Override public void setTeam(Team team) { this.team = team; }
    @Override public Employee getOwner() { return owner; }
    @Override public void setOwner(Employee owner) { this.owner = owner; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public ProjectStage getStage() { return stage; }
    public void setStage(ProjectStage stage) { this.stage = stage; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Credentials getCredential() { return credential; }
    public void setCredential(Credentials credential) { this.credential = credential; }
    public CustomerFeedbackKind getKind() { return kind; }
    public void setKind(CustomerFeedbackKind kind) { this.kind = kind; }
    public CustomerFeedbackType getType() { return type; }
    public void setType(CustomerFeedbackType type) { this.type = type; }
    public CustomerFeedbackStatus getStatus() { return status; }
    public void setStatus(CustomerFeedbackStatus status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Employee getHandler() { return handler; }
    public void setHandler(Employee handler) { this.handler = handler; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public Integer getSatisfaction() { return satisfaction; }
    public void setSatisfaction(Integer satisfaction) { this.satisfaction = satisfaction; }
}
