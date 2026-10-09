package com.th4.edge.decoboon.material.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.material.configure.SelectionStatus;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectOwned;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
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
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.List;

/**
 * 签约后主材选材确认记录。
 *
 * <p>每一条记录对应一个合同主材分项的一个结果版本：设计师逐项维护可读文本与 JSON 快照，
 * 客户逐项确认；已确认版本不覆盖，变更形成新版本，不反向修改报价、合同、成本、材料计划和采购事实。</p>
 */
@Note("主材选材记录")
@Description("项目合同主材分项的选择结果版本，按主材分项保留历史确认")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_material_selection",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_material_selection_item_version",
                        columnNames = {"com_id", "source_contract_line_id", "version"})
        },
        indexes = {
                @Index(name = "idx_material_selection_company", columnList = "com_id, selection_id"),
                @Index(name = "idx_material_selection_company_property", columnList = "com_id, property_id, selection_id"),
                @Index(name = "idx_material_selection_company_contract", columnList = "com_id, contract_id, selection_id"),
                @Index(name = "idx_material_selection_company_project", columnList = "com_id, project_id, selection_id"),
                @Index(name = "idx_material_selection_company_item", columnList = "com_id, source_contract_line_id, selection_id"),
                @Index(name = "idx_material_selection_company_store", columnList = "com_id, store_id, selection_id"),
                @Index(name = "idx_material_selection_company_team", columnList = "com_id, team_id, selection_id"),
                @Index(name = "idx_material_selection_company_owner", columnList = "com_id, owner_id, selection_id")
        })
public class MaterialSelection extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("选材记录索引")
    private Long selectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("归属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Note("归属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @Note("经营负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @Note("客户")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("房屋（选材唯一归属，多房屋客户必须显式指定）")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    @Note("关联合同（签约后主归属，项目未创建时可仅绑定房屋与合同）")
    private ProjectContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @Note("关联项目（项目创建后建立引用）")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_contract_line_id")
    @Note("来源合同主材分项（空值兼容旧版整单选材记录）")
    private ContractLine sourceContractLine;

    @Column(nullable = false)
    @Note("版本号（同一分项从 1 递增；进入采购后快照不再回写）")
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("选材状态")
    private SelectionStatus status = SelectionStatus.Selected;

    @Keyword
    @Column(length = 128)
    @Note("选材标题")
    private String title;

    @Lob
    @Column(nullable = false)
    @Note("选材正文文本（颜色、规格、大小等确认结果的自由文本，不做逐材料结构化解析）")
    private String content;

    @Lob
    @Note("选材结果 JSON 快照（用于界面回填和后续导出，不作为金额、成本或采购计算依据）")
    private String selectionJson;

    @ElementCollection
    @CollectionTable(name = "db_material_selection_attachments", joinColumns = @JoinColumn(name = "selection_id"))
    @Column(name = "url", length = 512)
    @Note("选材图片或附件引用")
    private List<String> attachments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    @Note("维护设计师")
    private Employee designer;

    @Column(length = 500)
    @Note("备注")
    private String remark;

    public Long getSelectionId() {
        return selectionId;
    }

    public void setSelectionId(Long selectionId) {
        this.selectionId = selectionId;
    }

    @Override
    public Company getCompany() {
        return company;
    }

    @Override
    public void setCompany(Company company) {
        this.company = company;
    }

    @Override
    public Store getStore() {
        return store;
    }

    @Override
    public void setStore(Store store) {
        this.store = store;
    }

    @Override
    public Team getTeam() {
        return team;
    }

    @Override
    public void setTeam(Team team) {
        this.team = team;
    }

    @Override
    public Employee getOwner() {
        return owner;
    }

    @Override
    public void setOwner(Employee owner) {
        this.owner = owner;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public ProjectContract getContract() {
        return contract;
    }

    public void setContract(ProjectContract contract) {
        this.contract = contract;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ContractLine getSourceContractLine() {
        return sourceContractLine;
    }

    public void setSourceContractLine(ContractLine sourceContractLine) {
        this.sourceContractLine = sourceContractLine;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public SelectionStatus getStatus() {
        return status;
    }

    public void setStatus(SelectionStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSelectionJson() {
        return selectionJson;
    }

    public void setSelectionJson(String selectionJson) {
        this.selectionJson = selectionJson;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public Employee getDesigner() {
        return designer;
    }

    public void setDesigner(Employee designer) {
        this.designer = designer;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
