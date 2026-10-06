package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, Long>, JpaSpecificationExecutor<AttendanceSummary> {
    Optional<AttendanceSummary> findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(Long comId, Long empId, String month);
    List<AttendanceSummary> findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(Long comId, String month);
}
