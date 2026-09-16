package com.th4.edge.tenant.billing.model;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.framework.data.model.prepare.annotation.UpdateTime;
import com.th4.edge.tenant.billing.configure.SubStatus;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 企业订阅。
 *
 * <p>企业计费主体，保存当前套餐、可用天数余额、服务到期时间与试用标记。
 * 余额是读模型快照，事实来源为订单与余额流水。</p>
 */
@Note("企业订阅")
@Description("企业计费主体，承载套餐、可用天数和到期状态")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_subscription")
public class Subscription extends AbstractModel {

    @Note("订阅索引")
    @Id
    @SnowflakeId
    private Long subId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false, unique = true)
    private Company company;

    @Note("当前套餐")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Note("当前生效套餐版本")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_version_id")
    private PlanVersion planVersion;

    @Note("订阅状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private SubStatus status = SubStatus.Trial;

    @Note("服务到期时间")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dateTime", column = @Column(name = "service_end_date_time")),
            @AttributeOverride(name = "date", column = @Column(name = "service_end_date")),
            @AttributeOverride(name = "time", column = @Column(name = "service_end_time")),
            @AttributeOverride(name = "year", column = @Column(name = "service_end_year")),
            @AttributeOverride(name = "month", column = @Column(name = "service_end_month")),
            @AttributeOverride(name = "day", column = @Column(name = "service_end_day")),
            @AttributeOverride(name = "hour", column = @Column(name = "service_end_hour")),
            @AttributeOverride(name = "minute", column = @Column(name = "service_end_minute")),
            @AttributeOverride(name = "second", column = @Column(name = "service_end_second")),
            @AttributeOverride(name = "week", column = @Column(name = "service_end_week"))
    })
    private DateTime serviceEndTime;

    @Note("是否已领取试用")
    @Column(nullable = false)
    private Boolean trialActivated = Boolean.FALSE;

    @Note("最近续费时间")
    @Column(name = "last_renew_time")
    private LocalDateTime lastRenewTime;

    @Note("已加购席位数")
    @Column(name = "extra_seats", nullable = false)
    private Integer extraSeats = 0;

    @Note("已加购门店数")
    @Column(name = "extra_stores", nullable = false)
    private Integer extraStores = 0;

    @Note("剩余可用天数")
    @Transient
    private Integer remainingDays;

    @Note("已使用席位数")
    @Transient
    private Long usedSeats;

    @Note("已使用门店数")
    @Transient
    private Long usedStores;

    @Note("生效最大席位数")
    @Transient
    private Integer effectiveMaxSeats;

    @Note("生效最大门店数")
    @Transient
    private Integer effectiveMaxStores;

    @Note("当前是否允许写入")
    @Transient
    private Boolean writable;

    @Note("订阅是否已到期")
    @Transient
    private Boolean expired;

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

    public Long getSubId() {
        return subId;
    }

    public void setSubId(Long subId) {
        this.subId = subId;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public PlanVersion getPlanVersion() {
        return planVersion;
    }

    public void setPlanVersion(PlanVersion planVersion) {
        this.planVersion = planVersion;
    }

    public SubStatus getStatus() {
        return status;
    }

    public void setStatus(SubStatus status) {
        this.status = status;
    }

    public DateTime getServiceEndTime() {
        return serviceEndTime;
    }

    public void setServiceEndTime(DateTime serviceEndTime) {
        this.serviceEndTime = serviceEndTime;
    }

    public Boolean getTrialActivated() {
        return trialActivated;
    }

    public void setTrialActivated(Boolean trialActivated) {
        this.trialActivated = trialActivated;
    }

    public LocalDateTime getLastRenewTime() {
        return lastRenewTime;
    }

    public void setLastRenewTime(LocalDateTime lastRenewTime) {
        this.lastRenewTime = lastRenewTime;
    }

    public Integer getExtraSeats() {
        return extraSeats;
    }

    public void setExtraSeats(Integer extraSeats) {
        this.extraSeats = extraSeats;
    }

    public Integer getExtraStores() {
        return extraStores;
    }

    public void setExtraStores(Integer extraStores) {
        this.extraStores = extraStores;
    }

    public Integer getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(Integer remainingDays) {
        this.remainingDays = remainingDays;
    }

    public Long getUsedSeats() {
        return usedSeats;
    }

    public void setUsedSeats(Long usedSeats) {
        this.usedSeats = usedSeats;
    }

    public Long getUsedStores() {
        return usedStores;
    }

    public void setUsedStores(Long usedStores) {
        this.usedStores = usedStores;
    }

    public Integer getEffectiveMaxSeats() {
        return effectiveMaxSeats;
    }

    public void setEffectiveMaxSeats(Integer effectiveMaxSeats) {
        this.effectiveMaxSeats = effectiveMaxSeats;
    }

    public Integer getEffectiveMaxStores() {
        return effectiveMaxStores;
    }

    public void setEffectiveMaxStores(Integer effectiveMaxStores) {
        this.effectiveMaxStores = effectiveMaxStores;
    }

    public Boolean getWritable() {
        return writable;
    }

    public void setWritable(Boolean writable) {
        this.writable = writable;
    }

    public Boolean getExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }

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
