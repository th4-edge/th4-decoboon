package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.PayableSettlement;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 应付款核销明细仓储。 */
public interface PayableSettlementRepository extends JpaRepository<PayableSettlement, Long> {

    List<PayableSettlement> findByPayment_PaymentId(Long paymentId);

    List<PayableSettlement> findByPayable_PayableId(Long payableId);
}
