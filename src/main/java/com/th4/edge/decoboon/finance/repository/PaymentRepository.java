package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 付款仓储。 */
public interface PaymentRepository extends JpaRepository<Payment, Long>,
        JpaSpecificationExecutor<Payment> {

    List<Payment> findByCompany_ComIdAndProject_ProjectIdOrderByPaidAtDesc(Long comId, Long projectId);

    List<Payment> findByCompany_ComIdOrderByPaidAtDesc(Long comId);

    List<Payment> findTop200ByCompany_ComIdOrderByPaidAtDesc(Long comId);
}
