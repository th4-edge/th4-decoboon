package com.th4.edge.tenant.billing.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.framework.data.date.model.DateTime;
import jakarta.persistence.*;

/**
 * 套餐版本快照。
 */
@Note("套餐版本")
@Description("固化套餐价格、周期、门店、席位和功能模块，避免后续调价影响历史订单")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_plan_version", uniqueConstraints = {
        @UniqueConstraint(name = "uk_plan_version", columnNames = {"plan_id", "version_no"})
}, indexes = {
        @Index(name = "idx_plan_version_plan", columnList = "plan_id, version_no")
})
public class PlanVersion extends AbstractModel {

    @Note("版本索引")
    @Id
    @SnowflakeId
    private Long versionId;

    @Note("所属套餐")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Note("版本号")
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Note("套餐编码快照")
    @Column(name = "plan_code", length = 32, nullable = false)
    private String planCode;

    @Note("套餐名称快照")
    @Column(name = "plan_name", length = 64, nullable = false)
    private String planName;

    @Note("有效天数")
    @Unit("天")
    @Column(nullable = false)
    private Integer days;

    @Note("最大门店数")
    @Unit("家")
    @Column(name = "max_stores", nullable = false)
    private Integer maxStores;

    @Note("最大席位数")
    @Unit("个")
    @Column(name = "max_seats", nullable = false)
    private Integer maxSeats;

    @Note("功能模块")
    @Column(length = 512, nullable = false)
    private String modules;

    @Note("销售价")
    @Unit("分")
    @Column(nullable = false)
    private Long price;

    @Note("原价")
    @Unit("分")
    private Long originalPrice;

    @Note("套餐说明")
    @Column(length = 512)
    private String description;

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

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getMaxStores() { return maxStores; }
    public void setMaxStores(Integer maxStores) { this.maxStores = maxStores; }
    public Integer getMaxSeats() { return maxSeats; }
    public void setMaxSeats(Integer maxSeats) { this.maxSeats = maxSeats; }
    public String getModules() { return modules; }
    public void setModules(String modules) { this.modules = modules; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Long originalPrice) { this.originalPrice = originalPrice; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
}
