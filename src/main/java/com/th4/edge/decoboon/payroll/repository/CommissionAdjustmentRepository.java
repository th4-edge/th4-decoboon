package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.configure.CommissionAdjustmentStatus;
import com.th4.edge.decoboon.payroll.model.CommissionAdjustment;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CommissionAdjustmentRepository extends JpaRepository<CommissionAdjustment, Long>,
        JpaSpecificationExecutor<CommissionAdjustment> {

    List<CommissionAdjustment> findByCompany_ComIdAndOriginalCommission_CommissionIdOrderByAdjustmentIdAsc(
            Long comId, Long commissionId);

    List<CommissionAdjustment> findByCompany_ComIdAndEmployee_EmpIdAndStatusOrderByOccurredAtAscAdjustmentIdAsc(
            Long comId, Long empId, CommissionAdjustmentStatus status);

    List<CommissionAdjustment> findByCompany_ComIdAndPayrollItem_ItemIdAndStatus(
            Long comId, Long itemId, CommissionAdjustmentStatus status);

    Optional<CommissionAdjustment> findByCompany_ComIdAndIdempotencyKey(Long comId, String idempotencyKey);

    Optional<CommissionAdjustment> findByCompany_ComIdAndOriginalCommission_CommissionIdAndRefund_RefundId(
            Long comId, Long commissionId, Long refundId);

    List<CommissionAdjustment> findByCompany_ComIdAndProject_ProjectId(Long comId, Long projectId);

}
