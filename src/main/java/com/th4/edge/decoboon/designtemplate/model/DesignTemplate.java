package com.th4.edge.decoboon.designtemplate.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;

/**
 * 设计模版（方案库）模版主档。
 *
 * <p>只承载**身份与治理**：归属、范围、状态、当前版本指针。
 * 模版的名称、房型、适用面积、标签等内容事实一律放在
 * {@link DesignTemplateVersion}，因为已发布版本必须不可变（BR-040 §5 / DEC-050）。</p>
 *
 * <p>本实体不承载任何价格字段。</p>
 */
@Note("设计模版")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "design_template")
public class DesignTemplate extends AuditModel {

    /** 模版归属范围：个人 / 门店 / 企业。 */
    public enum Scope implements Enumerable<String> {
        Personal("个人"), Store("门店"), Company("企业");

        private final String name;

        Scope(String name) { this.name = name; }

        @Override public String getName() { return name; }

        @Override public String getValue() { return toString(); }
    }

    /** 模版状态：草稿 / 已发布（存在已发布版本）/ 已停用。 */
    public enum Status implements Enumerable<String> {
        Draft("草稿"), Published("已发布"), Disabled("已停用");

        private final String name;

        Status(String name) { this.name = name; }

        @Override public String getName() { return name; }

        @Override public String getValue() { return toString(); }
    }

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("归属人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Employee owner;

    @Note("模版")
    @Id
    @SnowflakeId
    private Long templateId;

    @Note("归属范围")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Scope scope = Scope.Personal;

    @Note("状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status = Status.Draft;

    @Note("当前版本（草稿或已发布）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private DesignTemplateVersion currentVersion;

    @Note("派生来源模版（新版本由哪个模版派生）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_template_id")
    private DesignTemplate sourceTemplate;

    @Note("备注")
    @Column(length = 512)
    private String remark;

    @Note("区域骨架（详情响应组装，不落库）")
    @Transient
    private List<DesignTemplateRoom> rooms = new ArrayList<>();

    @Note("分项（详情响应组装，不落库；名称/单位/价格读 catalogItem）")
    @Transient
    private List<DesignTemplateItem> items = new ArrayList<>();

    public Company getCompany() { return company; }

    public void setCompany(Company company) { this.company = company; }

    public Store getStore() { return store; }

    public void setStore(Store store) { this.store = store; }

    public Employee getOwner() { return owner; }

    public void setOwner(Employee owner) { this.owner = owner; }

    public Long getTemplateId() { return templateId; }

    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Scope getScope() { return scope; }

    public void setScope(Scope scope) { this.scope = scope; }

    public Status getStatus() { return status; }

    public void setStatus(Status status) { this.status = status; }

    public DesignTemplateVersion getCurrentVersion() { return currentVersion; }

    public void setCurrentVersion(DesignTemplateVersion currentVersion) { this.currentVersion = currentVersion; }

    public DesignTemplate getSourceTemplate() { return sourceTemplate; }

    public void setSourceTemplate(DesignTemplate sourceTemplate) { this.sourceTemplate = sourceTemplate; }

    public String getRemark() { return remark; }

    public void setRemark(String remark) { this.remark = remark; }

    public List<DesignTemplateRoom> getRooms() { return rooms; }

    public void setRooms(List<DesignTemplateRoom> rooms) { this.rooms = rooms; }

    public List<DesignTemplateItem> getItems() { return items; }

    public void setItems(List<DesignTemplateItem> items) { this.items = items; }
}
