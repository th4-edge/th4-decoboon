package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

/** 工地动态与施工日报。 */
@Note("工地动态")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_project_report", indexes = {
        @Index(name = "idx_project_report_company", columnList = "com_id, report_id"),
        @Index(name = "idx_project_report_company_project", columnList = "com_id, project_id, report_id"),
        @Index(name = "idx_project_report_company_author", columnList = "com_id, emp_id, report_id")
,
        @Index(name = "idx_project_report_company_store", columnList = "com_id, store_id, report_id"),
        @Index(name = "idx_project_report_company_team", columnList = "com_id, team_id, report_id"),
        @Index(name = "idx_project_report_company_owner", columnList = "com_id, owner_id, report_id")
})
public class ProjectReport extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("动态索引")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
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
    @Note("施工阶段")
    private ProjectStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    @Note("施工任务")
    private ProjectTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_id")
    @Note("关联变更单")
    private ContractChange change;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("动态类型")
    private ReportType type = ReportType.Progress;

    @Keyword
    @Column(length = 128, nullable = false)
    @Note("标题")
    private String title;

    @Keyword
    @Column(length = 1024, nullable = false)
    @Note("内容")
    private String content;

    @ElementCollection
    @CollectionTable(name = "db_project_report_image", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "url", length = 512)
    @Note("现场图片")
    private List<String> images;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @Note("发布员工")
    private Employee author;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("发布角色")
    private EmployeeRole authorRole;

    @Column(nullable = false)
    @Note("业主是否可见")
    private Boolean visibleToCustomer = Boolean.TRUE;

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public ProjectStage getStage() { return stage; }
    public void setStage(ProjectStage stage) { this.stage = stage; }
    public ProjectTask getTask() { return task; }
    public void setTask(ProjectTask task) { this.task = task; }
    public ContractChange getChange() { return change; }
    public void setChange(ContractChange change) { this.change = change; }
    public ReportType getType() { return type; }
    public void setType(ReportType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Employee getAuthor() { return author; }
    public void setAuthor(Employee author) { this.author = author; }
    public EmployeeRole getAuthorRole() { return authorRole; }
    public void setAuthorRole(EmployeeRole authorRole) { this.authorRole = authorRole; }
    public Boolean getVisibleToCustomer() { return visibleToCustomer; }
    public void setVisibleToCustomer(Boolean visibleToCustomer) { this.visibleToCustomer = visibleToCustomer; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
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
