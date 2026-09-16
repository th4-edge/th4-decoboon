package com.th4.edge.tenant.billing.model;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.framework.data.model.prepare.annotation.UpdateTime;
import jakarta.persistence.*;

/**
 * 套餐。
 */
@Note("套餐")
@Description("企业可购买或领取的服务套餐")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_plan")
public class Plan extends AbstractModel {

    @Note("套餐索引")
    @Id
    @SnowflakeId
    private Long planId;

    @Note("套餐编码")
    @Keyword
    @Column(length = 32, nullable = false, unique = true)
    private String code;

    @Note("套餐名称")
    @Keyword
    @Column(length = 64, nullable = false)
    private String name;

    @Note("有效天数")
    @Unit("天")
    @Column(nullable = false)
    private Integer days;

    @Note("最大门店数")
    @Description("1 表示仅默认总店；大于 1 表示启用多门店")
    @Unit("家")
    @Column(nullable = false)
    private Integer maxStores = 1;

    @Note("最大席位数")
    @Description("企业可使用的员工账号数，包含企业负责人")
    @Unit("个")
    @Column(name = "max_seats")
    private Integer maxSeats;

    @Note("功能模块")
    @Description("逗号分隔的 PlanModule 编码；为空表示兼容历史套餐，不限制模块")
    @Column(length = 512)
    private String modules;

    @Note("销售价")
    @Unit("分")
    @Column(nullable = false)
    private Long price;

    @Note("原价")
    @Unit("分")
    private Long originalPrice;

    @Note("是否仅可购买/领取一次")
    @Column(nullable = false)
    private Boolean oneTimeOnly = Boolean.FALSE;

    @Note("是否上架")
    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Note("排序")
    @Column(nullable = false)
    private Integer priority = 0;

    @Note("套餐说明")
    @Keyword
    @Column(length = 512)
    private String description;

    @Note("营销短句")
    @Column(length = 256)
    private String tagline;

    @Note("权益亮点")
    @Description("分号分隔的营销权益点，用于套餐卡片展示")
    @Column(length = 1024)
    private String highlights;

    @Note("角标文案")
    @Column(length = 32)
    private String badge;

    @Note("是否推荐")
    @Column(nullable = false)
    private Boolean recommended = Boolean.FALSE;

    @Note("创建时间")
    @CreateTime
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dateTime", column = @Column(name = "create_date_time")),
            @AttributeOverride(name = "date", column = @Column(name = "create_date")),
            @AttributeOverride(name = "time", column = @Column(name = "create_time")),
            @AttributeOverride(name = "year", column = @Column(name = "create_year")),
            @AttributeOverride(name = "month", column = @Column(name = "create_month")),
            @AttributeOverride(name = "day", column = @Column(name = "create_day")),
            @AttributeOverride(name = "hour", column = @Column(name = "create_hour")),
            @AttributeOverride(name = "minute", column = @Column(name = "create_minute")),
            @AttributeOverride(name = "second", column = @Column(name = "create_second")),
            @AttributeOverride(name = "week", column = @Column(name = "create_week"))
    })
    private DateTime createTime;

    @Note("更新时间")
    @UpdateTime
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dateTime", column = @Column(name = "update_date_time")),
            @AttributeOverride(name = "date", column = @Column(name = "update_date")),
            @AttributeOverride(name = "time", column = @Column(name = "update_time")),
            @AttributeOverride(name = "year", column = @Column(name = "update_year")),
            @AttributeOverride(name = "month", column = @Column(name = "update_month")),
            @AttributeOverride(name = "day", column = @Column(name = "update_day")),
            @AttributeOverride(name = "hour", column = @Column(name = "update_hour")),
            @AttributeOverride(name = "minute", column = @Column(name = "update_minute")),
            @AttributeOverride(name = "second", column = @Column(name = "update_second")),
            @AttributeOverride(name = "week", column = @Column(name = "update_week"))
    })
    private DateTime updateTime;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Integer getMaxStores() {
        return maxStores;
    }

    public void setMaxStores(Integer maxStores) {
        this.maxStores = maxStores;
    }

    public Integer getMaxSeats() {
        return maxSeats;
    }

    public void setMaxSeats(Integer maxSeats) {
        this.maxSeats = maxSeats;
    }

    public String getModules() {
        return modules;
    }

    public void setModules(String modules) {
        this.modules = modules;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Long getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Long originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Boolean getOneTimeOnly() {
        return oneTimeOnly;
    }

    public void setOneTimeOnly(Boolean oneTimeOnly) {
        this.oneTimeOnly = oneTimeOnly;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public Boolean getRecommended() { return recommended; }
    public void setRecommended(Boolean recommended) { this.recommended = recommended; }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }

    public DateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(DateTime updateTime) {
        this.updateTime = updateTime;
    }
}
