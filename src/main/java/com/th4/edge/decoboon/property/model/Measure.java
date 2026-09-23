package com.th4.edge.decoboon.property.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
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

import java.time.LocalDateTime;
import java.util.List;

/** 房屋量房记录。 */
@Note("量房记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_measure", indexes = {
        @Index(name = "idx_measure_company", columnList = "com_id, measure_id"),
        @Index(name = "idx_measure_company_property", columnList = "com_id, property_id, measure_id"),
        @Index(name = "idx_measure_company_customer", columnList = "com_id, customer_id, measure_id"),
        @Index(name = "idx_measure_company_designer", columnList = "com_id, designer_id, measure_id"),
        @Index(name = "idx_measure_company_executor", columnList = "com_id, executor_id, measure_id")
})
@DataPermission
public class Measure extends AuditModel {

    @Id
    @SnowflakeId
    @Note("量房索引")
    private Long measureId;

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
    @JoinColumn(name = "emp_id", nullable = false)
    @Note("数据负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @Note("客户")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    @Note("房屋")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    @Note("量房设计师（设计责任人事实，未指派时为空）")
    private Employee designer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executor_id")
    @Note("量房执行人（实际上门执行量房的员工，与设计师事实分离）")
    private Employee executor;

    @Note("计划上门时间（房屋级预约事实）")
    private LocalDateTime scheduledAt;

    @Note("实际完成时间（量房完成事实）")
    private LocalDateTime completedAt;

    @Note("量房时间（兼容投影：Scheduled=scheduledAt，Completed=completedAt）")
    private LocalDateTime measureTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("量房状态")
    private MeasureStatus status = MeasureStatus.Scheduled;

    @ElementCollection
    @CollectionTable(name = "db_measure_site_photos", joinColumns = @JoinColumn(name = "measure_id"))
    @Column(name = "url", length = 512)
    @Note("现场照片")
    private List<String> sitePhotos;

    @ElementCollection
    @CollectionTable(name = "db_measure_structure_images", joinColumns = @JoinColumn(name = "measure_id"))
    @Column(name = "url", length = 512)
    @Note("结构图纸")
    private List<String> structureImages;

    @ElementCollection
    @CollectionTable(name = "db_measure_attachments", joinColumns = @JoinColumn(name = "measure_id"))
    @Column(name = "url", length = 512)
    @Note("量房附件")
    private List<String> attachments;

    @Lob
    @Column
    @Note("原始结构说明")
    private String originalStructure;

    @Lob
    @Column
    @Note("量房尺寸")
    private String dimensions;

    @Lob
    @Column
    @Note("客户诉求")
    private String customerRequirements;

    @Lob
    @Column
    @Note("设计师结论")
    private String designerConclusion;

    public Long getMeasureId() { return measureId; }
    public void setMeasureId(Long measureId) { this.measureId = measureId; }
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
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public Employee getDesigner() { return designer; }
    public void setDesigner(Employee designer) { this.designer = designer; }
    public Employee getExecutor() { return executor; }
    public void setExecutor(Employee executor) { this.executor = executor; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getMeasureTime() { return measureTime; }
    public void setMeasureTime(LocalDateTime measureTime) { this.measureTime = measureTime; }
    public MeasureStatus getStatus() { return status; }
    public void setStatus(MeasureStatus status) { this.status = status; }
    public List<String> getSitePhotos() { return sitePhotos; }
    public void setSitePhotos(List<String> sitePhotos) { this.sitePhotos = sitePhotos; }
    public List<String> getStructureImages() { return structureImages; }
    public void setStructureImages(List<String> structureImages) { this.structureImages = structureImages; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
    public String getOriginalStructure() { return originalStructure; }
    public void setOriginalStructure(String originalStructure) { this.originalStructure = originalStructure; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public String getCustomerRequirements() { return customerRequirements; }
    public void setCustomerRequirements(String customerRequirements) { this.customerRequirements = customerRequirements; }
    public String getDesignerConclusion() { return designerConclusion; }
    public void setDesignerConclusion(String designerConclusion) { this.designerConclusion = designerConclusion; }
}
