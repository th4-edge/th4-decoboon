package com.th4.edge.decoboon.project.model;
import com.th4.edge.decoboon.property.model.Property;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.field.annotation.LogicField;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 项目施工任务。 */
@Note("项目施工任务")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_project_task",
        indexes = {
                @Index(name = "idx_project_task_company", columnList = "com_id, task_id"),
                @Index(name = "idx_project_task_company_store", columnList = "com_id, store_id, task_id"),
                @Index(name = "idx_project_task_company_team", columnList = "com_id, team_id, task_id"),
                @Index(name = "idx_project_task_company_owner", columnList = "com_id, owner_id, task_id")
        }
)
public class ProjectTask extends AuditModel implements ProjectOwned {

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


    @Id
    @SnowflakeId
    @Note("任务索引")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Note("所属项目")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("所属房屋")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    @Note("所属阶段")
    private ProjectStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_line_id")
    @Note("套餐分项")
    private PackageLine packageLine;

    @Note("报价项目库来源")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private CatalogItem catalogItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_item_id")
    @Note("设计报价施工分项")
    private QuoteLine quoteItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_line_id")
    @Note("来源合同行")
    private ContractLine contractLine;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_item_id")
    @Note("来源变更行")
    private ContractChangeItem changeItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Note("任务负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @Note("创建人")
    private Employee creator;

    @Keyword
    @Column(length = 128, nullable = false)
    @Note("任务名称")
    private String name;

    @Keyword
    @Column(length = 32)
    @Note("施工区域")
    private String zone;

    @Column(length = 512)
    @Note("施工工艺")
    private String craft;

    @Column(length = 512)
    @Note("规格要求")
    private String spec;

    @Column(precision = 12, scale = 2, nullable = false)
    @Note("施工数量")
    private BigDecimal quantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    @Note("计量单位")
    private Unit unit = Unit.Area;

    @Column(precision = 12, scale = 2, nullable = false)
    @Note("销售金额")
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    @Note("成本金额")
    private BigDecimal cost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("任务状态")
    private TaskStatus status = TaskStatus.Todo;

    @LogicField
    @Column(nullable = false)
    @Note("任务进度")
    private Integer progress = 0;

    @Column(length = 16)
    @Note("优先级")
    private String priority = "normal";

    @Column(nullable = false)
    @Note("是否包含隐蔽节点")
    private Boolean conceal = Boolean.FALSE;

    @Column(length = 512)
    @Note("任务备注")
    private String remark;

    @Note("计划开始日期")
    private LocalDate plannedStart;

    @Note("计划结束日期")
    private LocalDate plannedEnd;

    @Note("实际开始日期")
    private LocalDate actualStart;

    @Note("实际结束日期")
    private LocalDate actualEnd;

    @ElementCollection
    @CollectionTable(name = "db_decoboon_project_task_evidence", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "url", length = 512)
    @Note("施工证据")
    private List<String> evidenceUrls;

    @Transient
    @Note("任务节点")
    private List<TaskNode> nodes;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public ProjectStage getStage() { return stage; }
    public void setStage(ProjectStage stage) { this.stage = stage; }
    public PackageLine getPackageLine() { return packageLine; }
    public void setPackageLine(PackageLine packageLine) { this.packageLine = packageLine; }
    public QuoteLine getQuoteItem() { return quoteItem; }
    public void setQuoteItem(QuoteLine quoteItem) { this.quoteItem = quoteItem; }
    public CatalogItem getCatalogItem() { return catalogItem; }
    public void setCatalogItem(CatalogItem catalogItem) { this.catalogItem = catalogItem; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Employee getCreator() { return creator; }
    public void setCreator(Employee creator) { this.creator = creator; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getCraft() { return craft; }
    public void setCraft(String craft) { this.craft = craft; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Boolean getConceal() { return conceal; }
    public void setConceal(Boolean conceal) { this.conceal = conceal; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDate plannedStart) { this.plannedStart = plannedStart; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDate plannedEnd) { this.plannedEnd = plannedEnd; }
    public LocalDate getActualStart() { return actualStart; }
    public void setActualStart(LocalDate actualStart) { this.actualStart = actualStart; }
    public LocalDate getActualEnd() { return actualEnd; }
    public void setActualEnd(LocalDate actualEnd) { this.actualEnd = actualEnd; }
    public List<String> getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(List<String> evidenceUrls) { this.evidenceUrls = evidenceUrls; }
    public List<TaskNode> getNodes() { return nodes; }
    public void setNodes(List<TaskNode> nodes) { this.nodes = nodes; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }

    public ContractLine getContractLine() { return contractLine; }
    public void setContractLine(ContractLine contractLine) { this.contractLine = contractLine; }
    @Override
    public Store getStore() { return store; }

    @Override
    public void setStore(Store store) { this.store = store; }

    @Override
    public Team getTeam() { return team; }

    @Override
    public void setTeam(Team team) { this.team = team; }


    public ContractChangeItem getChangeItem() { return changeItem; }

    public void setChangeItem(ContractChangeItem changeItem) { this.changeItem = changeItem; }


}
