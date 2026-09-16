package com.th4.edge.tenant.billing.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.billing.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 订单仓储。
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Order findByOrderNo(String orderNo);

    Page<Order> findByCompany_ComIdOrderByOrderIdDesc(Long comId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderNo = :orderNo")
    Order findByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
