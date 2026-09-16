package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.Order;
import com.th4.edge.tenant.billing.model.OrderItem;

import java.util.List;

/**
 * 订单项仓储。
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_OrderIdOrderByOrderItemIdAsc(Long orderId);
}
