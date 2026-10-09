package com.th4.edge.decoboon.content.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.content.configure.ContentAttributionConfidence;
import com.th4.edge.decoboon.content.configure.ContentAttributionType;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** 内容线索触点。 */
@Note("内容线索触点")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_content_lead_touch", uniqueConstraints = {
        @UniqueConstraint(name = "uk_content_touch_customer_content", columnNames = {"customer_id", "employee_content_id"})
}, indexes = {
        @Index(name = "idx_content_touch_company", columnList = "com_id, touch_id"),
        @Index(name = "idx_content_touch_customer", columnList = "customer_id, touch_id")
})
public class LeadTouch extends AuditModel {

    @Id @SnowflakeId @Note("触点索引")
    private Long touchId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业")
    private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id", nullable = false) @Note("客户")
    private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_content_id", nullable = false) @Note("员工内容")
    private EmployeeContent employeeContent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "publication_id") @Note("发布记录")
    private PublicationRecord publication;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id", nullable = false) @Note("内容任务")
    private ContentTask task;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "emp_id", nullable = false) @Note("归因员工")
    private Employee employee;
    @Enumerated(EnumType.STRING) @Column(name = "attribution_type", length = 24, nullable = false) @Note("归因方式")
    private ContentAttributionType attributionType = ContentAttributionType.Manual;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) @Note("归因置信度")
    private ContentAttributionConfidence confidence = ContentAttributionConfidence.Medium;
    @Column(name = "is_primary", nullable = false) @Note("是否主归因")
    private Boolean isPrimary = true;
    @Column(length = 512) @Note("归因证据")
    private String evidence;
    @Column(name = "touched_at", nullable = false) @Note("触点时间")
    private LocalDateTime touchedAt;

    public Long getTouchId() { return touchId; }
    public void setTouchId(Long touchId) { this.touchId = touchId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public EmployeeContent getEmployeeContent() { return employeeContent; }
    public void setEmployeeContent(EmployeeContent employeeContent) { this.employeeContent = employeeContent; }
    public PublicationRecord getPublication() { return publication; }
    public void setPublication(PublicationRecord publication) { this.publication = publication; }
    public ContentTask getTask() { return task; }
    public void setTask(ContentTask task) { this.task = task; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public ContentAttributionType getAttributionType() { return attributionType; }
    public void setAttributionType(ContentAttributionType attributionType) { this.attributionType = attributionType; }
    public ContentAttributionConfidence getConfidence() { return confidence; }
    public void setConfidence(ContentAttributionConfidence confidence) { this.confidence = confidence; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public LocalDateTime getTouchedAt() { return touchedAt; }
    public void setTouchedAt(LocalDateTime touchedAt) { this.touchedAt = touchedAt; }
}
