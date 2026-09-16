package com.th4.edge.tenant.billing.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.billing.configure.OrderItemType;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.*;

/**
 * 订单项快照。
 */
@Note("订单项")
@Description("记录订单购买时的套餐或加购商品快照")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_order_item", indexes = {
        @Index(name = "idx_order_item_order", columnList = "order_id, order_item_id"),
        @Index(name = "idx_order_item_company", columnList = "com_id, order_item_id")
})
public class OrderItem extends AbstractModel {

    @Note("订单项索引")
    @Id
    @SnowflakeId
    private Long orderItemId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属订单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Note("订单项类型")
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 16, nullable = false)
    private OrderItemType itemType;

    @Note("套餐版本")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_version_id")
    private PlanVersion planVersion;

    @Note("加购商品")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_id")
    private PlanAddon addon;

    @Note("商品编码快照")
    @Column(name = "product_code", length = 32, nullable = false)
    private String productCode;

    @Note("商品名称快照")
    @Column(name = "product_name", length = 64, nullable = false)
    private String productName;

    @Note("单价快照")
    @Unit("分")
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Note("数量")
    @Column(nullable = false)
    private Integer quantity = 1;

    @Note("金额")
    @Unit("分")
    @Column(nullable = false)
    private Long amount;

    @Note("有效天数")
    @Unit("天")
    @Column(nullable = false)
    private Integer days;

    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public OrderItemType getItemType() { return itemType; }
    public void setItemType(OrderItemType itemType) { this.itemType = itemType; }
    public PlanVersion getPlanVersion() { return planVersion; }
    public void setPlanVersion(PlanVersion planVersion) { this.planVersion = planVersion; }
    public PlanAddon getAddon() { return addon; }
    public void setAddon(PlanAddon addon) { this.addon = addon; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
}
