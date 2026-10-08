package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** 客户状态变更日志仓储。 */
public interface CustomerStatusLogRepository extends JpaRepository<CustomerStatusLog, Long> {

    Optional<CustomerStatusLog> findByCompany_ComIdAndRequestId(Long comId, String requestId);

    Page<CustomerStatusLog> findByCompany_ComIdAndCustomer_CustIdOrderByLogIdDesc(
            Long comId, Long customerId, Pageable pageable);
}
