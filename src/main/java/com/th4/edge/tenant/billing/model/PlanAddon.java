package com.th4.edge.tenant.billing.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.billing.configure.PlanAddonType;
import jakarta.persistence.*;

/**
 * 套餐加购商品。
 */
@Note("套餐加购商品")
@Description("席位包、门店包等可独立加购的套餐权益")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_plan_addon")
public class PlanAddon extends AbstractModel {

    @Note("加购项索引")
    @Id
    @SnowflakeId
    private Long addonId;

    @Note("加购项编码")
    @Column(length = 32, nullable = false, unique = true)
    private String code;

    @Note("加购项名称")
    @Column(length = 64, nullable = false)
    private String name;

    @Note("加购类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private PlanAddonType type;

    @Note("单位价格")
    @Unit("分")
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Note("单位有效天数")
    @Unit("天")
    @Column(nullable = false)
    private Integer days;

    @Note("单次最大购买数量")
    @Column(name = "max_quantity", nullable = false)
    private Integer maxQuantity = 100;

    @Note("是否上架")
    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Note("排序")
    @Column(nullable = false)
    private Integer priority = 0;

    @Note("说明")
    @Column(length = 512)
    private String description;

    public Long getAddonId() { return addonId; }
    public void setAddonId(Long addonId) { this.addonId = addonId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PlanAddonType getType() { return type; }
    public void setType(PlanAddonType type) { this.type = type; }
    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
