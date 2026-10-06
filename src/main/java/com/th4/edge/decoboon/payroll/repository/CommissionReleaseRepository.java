package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.CommissionRelease;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface CommissionReleaseRepository extends JpaRepository<CommissionRelease, Long>, JpaSpecificationExecutor<CommissionRelease> {
    Optional<CommissionRelease> findByCompany_ComIdAndCommissionItem_CommissionIdAndReceipt_ReceiptId(Long comId, Long commissionId, Long receiptId);
    List<CommissionRelease> findByCommissionItem_CommissionIdOrderByReleasedAtAsc(Long commissionId);
}
