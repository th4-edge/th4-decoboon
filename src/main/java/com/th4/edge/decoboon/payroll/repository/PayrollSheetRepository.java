package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface PayrollSheetRepository extends JpaRepository<PayrollSheet, Long>, JpaSpecificationExecutor<PayrollSheet> {
    Optional<PayrollSheet> findByCompany_ComIdAndPeriod(Long comId, String period);
}
