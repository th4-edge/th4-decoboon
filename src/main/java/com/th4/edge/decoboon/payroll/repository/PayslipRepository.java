package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long>, JpaSpecificationExecutor<Payslip> {
    Optional<Payslip> findByCompany_ComIdAndSheet_SheetIdAndEmployee_EmpId(Long comId, Long sheetId, Long empId);
    List<Payslip> findByCompany_ComIdAndEmployee_EmpIdAndStatusAndPublishedAtIsNotNullOrderByPeriodDesc(
            Long comId, Long empId, com.th4.edge.decoboon.payroll.configure.PayslipStatus status);
}
