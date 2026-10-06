package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 订单仓储。
 *
 * <p>非加锁读取；支付履约的按订单号幂等由 {@code @DistributedLocked}（{@code order:{orderNo}}）串行化。</p>
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Order findByOrderNo(String orderNo);

    Page<Order> findByCompany_ComIdOrderByOrderIdDesc(Long comId, Pageable pageable);
}
