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
import com.th4.edge.tenant.billing.configure.OrderStatus;
import com.th4.edge.tenant.billing.configure.PayChannel;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 续费订单。
 */
@Note("续费订单")
@Description("套餐购买或试用领取的支付凭证")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_order")
public class Order extends AbstractModel {

    @Note("订单索引")
    @Id
    @SnowflakeId
    private Long orderId;

    @Note("订单号")
    @Keyword
    @Column(length = 40, nullable = false, unique = true)
    private String orderNo;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("购买套餐")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Note("购买套餐版本")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_version_id")
    private PlanVersion planVersion;

    @Note("订单金额")
    @Unit("分")
    @Column(nullable = false)
    private Long amount;

    @Note("到账天数")
    @Unit("天")
    @Column(nullable = false)
    private Integer days;

    @Note("订单状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private OrderStatus status = OrderStatus.Pending;

    @Note("支付渠道")
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PayChannel payChannel;

    @Note("外部交易号")
    @Column(name = "out_trade_no", length = 64)
    private String outTradeNo;

    @Note("微信支付交易号")
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Note("支付时间")
    @Column(name = "paid_time")
    private LocalDateTime paidTime;

    @Note("支付过期时间")
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Note("本次购买席位数")
    @Column(name = "extra_seats", nullable = false)
    private Integer extraSeats = 0;

    @Note("本次购买门店数")
    @Column(name = "extra_stores", nullable = false)
    private Integer extraStores = 0;

    @Note("订单项快照")
    @Transient
    private List<OrderItem> items;

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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PayChannel getPayChannel() {
        return payChannel;
    }

    public void setPayChannel(PayChannel payChannel) {
        this.payChannel = payChannel;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getPaidTime() {
        return paidTime;
    }

    public void setPaidTime(LocalDateTime paidTime) {
        this.paidTime = paidTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
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
