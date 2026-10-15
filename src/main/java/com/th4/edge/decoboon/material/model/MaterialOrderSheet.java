package com.th4.edge.decoboon.material.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.material.configure.MaterialSheetStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectOwned;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目主材下单作业单（作业单据层 / L3，对厂家的一张单）。
 *
 * <p>DEC-073：作业单与采购单是**两个对象、两张单**。作业单按品类成单（一个品类一张单），
 * 承载型号/颜色/尺寸/安装位置、测量·拉群·配送·安装时间与签字，**不携带价格、账期与成本**；
 * 1 张作业单可派生 N 张采购单。所有数量、金额、成本、状态最终落在事实层对象上（避免第二套账）。</p>
 *
 * <p>事实依据：SOL-014 / OBJ-008 / BR-044 / BR-045 / SCN-021 / DEC-072 / DEC-073。</p>
 */
@Note("主材下单作业单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_material_order_sheet",
        uniqueConstraints = @UniqueConstraint(name = "uk_mos_company_no", columnNames = {"com_id", "sheet_no"}),
        indexes = {
                @Index(name = "idx_mos_company", columnList = "com_id, sheet_id"),
                @Index(name = "idx_mos_company_project", columnList = "com_id, project_id, sheet_id"),
                @Index(name = "idx_mos_company_project_status", columnList = "com_id, project_id, status, sheet_id"),
                @Index(name = "idx_mos_company_store", columnList = "com_id, store_id, sheet_id"),
                @Index(name = "idx_mos_company_team", columnList = "com_id, team_id, sheet_id"),
                @Index(name = "idx_mos_company_owner", columnList = "com_id, owner_id, sheet_id")
        })
public class MaterialOrderSheet extends AuditModel implements ProjectOwned {

    @Note("作业单索引")
    @Id
    @SnowflakeId
    private Long sheetId;

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

    @Note("所属房屋")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Note("所属项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Note("所属合同")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ProjectContract contract;

    @Note("单据编号")
    @Column(name = "sheet_no", length = 64, nullable = false)
    private String sheetNo;

    @Note("单据类型编码（Tile 等）")
    @Column(name = "sheet_type", length = 64, nullable = false)
    private String sheetType;

    @Note("主材分类")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_category_id")
    private MaterialCategory materialCategory;

    @Note("供应商（设计师阶段可空，价格与供应商由财务决定）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Partner supplier;

    @Note("厂家名称文本")
    @Column(name = "manufacturer_name", length = 128)
    private String manufacturerName;

    @Note("测量时间")
    @Column(name = "measure_at")
    private LocalDate measureAt;

    @Note("拉群时间")
    @Column(name = "group_chat_at")
    private LocalDate groupChatAt;

    @Note("配送时间")
    @Column(name = "deliver_at")
    private LocalDate deliverAt;

    @Note("安装/送货时间")
    @Column(name = "install_at")
    private LocalDate installAt;

    @Note("所用单据模板")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private MaterialSheetTemplate template;

    @Note("生成时冻结的模板版本")
    @Column(name = "template_version")
    private Integer templateVersion;

    @Note("作业单状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private MaterialSheetStatus status = MaterialSheetStatus.Draft;

    @Note("价格状态（汇总态，行级为准）")
    @Enumerated(EnumType.STRING)
    @Column(name = "price_state", length = 16, nullable = false)
    private SheetPriceState priceState = SheetPriceState.Unpriced;

    @Note("签名位确认人与时间JSON")
    @Column(name = "signatures", columnDefinition = "LONGTEXT")
    private String signatures;

    @Note("备注")
    @Column(length = 500)
    private String remark;

    @Note("附件")
    @ElementCollection
    @CollectionTable(name = "db_material_order_sheet_attachments", joinColumns = @JoinColumn(name = "sheet_id"))
    @Column(name = "url", length = 512)
    private List<String> attachments;

    @Note("创建人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    @jakarta.persistence.Transient
    @Note("作业单行（分页/详情响应投影，不落库）")
    private List<MaterialOrderSheetItem> items;

    public List<MaterialOrderSheetItem> getItems() { return items; }
    public void setItems(List<MaterialOrderSheetItem> items) { this.items = items; }

    public Long getSheetId() { return sheetId; }
    public void setSheetId(Long sheetId) { this.sheetId = sheetId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public ProjectContract getContract() { return contract; }
    public void setContract(ProjectContract contract) { this.contract = contract; }
    public String getSheetNo() { return sheetNo; }
    public void setSheetNo(String sheetNo) { this.sheetNo = sheetNo; }
    public String getSheetType() { return sheetType; }
    public void setSheetType(String sheetType) { this.sheetType = sheetType; }
    public MaterialCategory getMaterialCategory() { return materialCategory; }
    public void setMaterialCategory(MaterialCategory materialCategory) { this.materialCategory = materialCategory; }
    public Partner getSupplier() { return supplier; }
    public void setSupplier(Partner supplier) { this.supplier = supplier; }
    public String getManufacturerName() { return manufacturerName; }
    public void setManufacturerName(String manufacturerName) { this.manufacturerName = manufacturerName; }
    public LocalDate getMeasureAt() { return measureAt; }
    public void setMeasureAt(LocalDate measureAt) { this.measureAt = measureAt; }
    public LocalDate getGroupChatAt() { return groupChatAt; }
    public void setGroupChatAt(LocalDate groupChatAt) { this.groupChatAt = groupChatAt; }
    public LocalDate getDeliverAt() { return deliverAt; }
    public void setDeliverAt(LocalDate deliverAt) { this.deliverAt = deliverAt; }
    public LocalDate getInstallAt() { return installAt; }
    public void setInstallAt(LocalDate installAt) { this.installAt = installAt; }
    public MaterialSheetTemplate getTemplate() { return template; }
    public void setTemplate(MaterialSheetTemplate template) { this.template = template; }
    public Integer getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(Integer templateVersion) { this.templateVersion = templateVersion; }
    public MaterialSheetStatus getStatus() { return status; }
    public void setStatus(MaterialSheetStatus status) { this.status = status; }
    public SheetPriceState getPriceState() { return priceState; }
    public void setPriceState(SheetPriceState priceState) { this.priceState = priceState; }
    public String getSignatures() { return signatures; }
    public void setSignatures(String signatures) { this.signatures = signatures; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
