package com.th4.edge.decoboon.project.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.OwnerPreviewResult;
import com.th4.edge.decoboon.project.configure.OwnerPreviewSource;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.configure.EmployeeRole;
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

import java.time.LocalDateTime;

/**
 * 员工打开业主视角只读预览的最小审计。
 *
 * <p>append-only：只记录员工、角色快照、企业、请求项目、来源、结果和打开时间，
 * 不记录业主点击、停留、访问频率或页面正文，也不作为业主已读或验收证据。</p>
 */
@Note("业主视角预览审计")
@Description("员工使用自身身份打开业主视角只读投影的最小安全审计")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_project_owner_preview_log", indexes = {
        @Index(name = "idx_owner_preview_company_project", columnList = "com_id, project_id, log_id"),
        @Index(name = "idx_owner_preview_company_requested", columnList = "com_id, requested_project_id, log_id"),
        @Index(name = "idx_owner_preview_company_employee", columnList = "com_id, employee_id, log_id"),
        @Index(name = "idx_owner_preview_company_opened", columnList = "com_id, opened_at")
})
public class ProjectOwnerPreviewLog extends AuditModel {

    @Id
    @SnowflakeId
    @Note("预览审计索引")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @Note("项目")
    private Project project;

    @Column(name = "requested_project_id", nullable = false)
    @Note("请求项目索引快照")
    private Long requestedProjectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @Note("打开预览的员工")
    private Employee employee;

    @Column(name = "employee_name", length = 15, nullable = false)
    @Note("员工姓名快照")
    private String employeeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_role", length = 24, nullable = false)
    @Note("打开时角色快照")
    private EmployeeRole employeeRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 16, nullable = false)
    @Note("来源")
    private OwnerPreviewSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 16, nullable = false)
    @Note("结果")
    private OwnerPreviewResult result;

    @Column(name = "opened_at", nullable = false)
    @Note("打开时间")
    private LocalDateTime openedAt = LocalDateTime.now();

    @Column(name = "request_id", length = 128)
    @Note("可选追踪号")
    private String requestId;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Long getRequestedProjectId() { return requestedProjectId; }
    public void setRequestedProjectId(Long requestedProjectId) { this.requestedProjectId = requestedProjectId; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public EmployeeRole getEmployeeRole() { return employeeRole; }
    public void setEmployeeRole(EmployeeRole employeeRole) { this.employeeRole = employeeRole; }
    public OwnerPreviewSource getSource() { return source; }
    public void setSource(OwnerPreviewSource source) { this.source = source; }
    public OwnerPreviewResult getResult() { return result; }
    public void setResult(OwnerPreviewResult result) { this.result = result; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
