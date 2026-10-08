package com.th4.edge.decoboon.designtemplate.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 设计模版分项。
 *
 * <p><b>唯一事实源是报价项目库</b>：名称、计价单位、计价方式、价格全部读
 * {@link CatalogItem}，本实体**不复制也不覆盖**，避免同名不同值的数据漂移
 * （BR-040 §3 / DEC-050 ⑤，2026-10-09 用户裁决 A）。</p>
 *
 * <p>本实体只承载模版独有的三件事：归属哪个区域、工程量公式、排序与备注。</p>
 */
@Note("设计模版分项")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "design_template_item")
public class DesignTemplateItem extends AuditModel {

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属模版版本")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private DesignTemplateVersion version;

    @Note("模版分项")
    @Id
    @SnowflakeId
    private Long itemId;

    @Note("归属区域键；为空表示全屋共享项")
    @Column(name = "room_key", length = 32)
    private String roomKey;

    @Note("引用报价项目库条目（名称/单位/计价方式/价格的唯一来源）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private CatalogItem catalogItem;

    @Note("工程量公式（专业变量）")
    @Column(name = "quantity_formula", length = 512)
    private String quantityFormula;

    @Note("无公式时的固定数量")
    @Column(name = "default_quantity", precision = 12, scale = 3)
    private BigDecimal defaultQuantity;

    @Note("展示顺序")
    @Column(name = "sort_no", nullable = false)
    private Integer sortNo = 0;

    @Note("备注")
    @Column(length = 256)
    private String remark;

    public Company getCompany() { return company; }

    public void setCompany(Company company) { this.company = company; }

    public DesignTemplateVersion getVersion() { return version; }

    public void setVersion(DesignTemplateVersion version) { this.version = version; }

    public Long getItemId() { return itemId; }

    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getRoomKey() { return roomKey; }

    public void setRoomKey(String roomKey) { this.roomKey = roomKey; }

    public CatalogItem getCatalogItem() { return catalogItem; }

    public void setCatalogItem(CatalogItem catalogItem) { this.catalogItem = catalogItem; }

    public String getQuantityFormula() { return quantityFormula; }

    public void setQuantityFormula(String quantityFormula) { this.quantityFormula = quantityFormula; }

    public BigDecimal getDefaultQuantity() { return defaultQuantity; }

    public void setDefaultQuantity(BigDecimal defaultQuantity) { this.defaultQuantity = defaultQuantity; }

    public Integer getSortNo() { return sortNo; }

    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

    public String getRemark() { return remark; }

    public void setRemark(String remark) { this.remark = remark; }
}
