package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, Long>, JpaSpecificationExecutor<AttendanceSummary> {
    Optional<AttendanceSummary> findByCompany_ComIdAndEmployee_EmpIdAndPayrollMonth(Long comId, Long empId, String month);
    List<AttendanceSummary> findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(Long comId, String month);
    boolean existsByCompany_ComIdAndEmployee_EmpId(Long comId, Long empId);

    @Query("""
            select distinct item.employee.empId from AttendanceSummary item
            where item.company.comId = :comId and item.employee.empId in :empIds
            """)
    Set<Long> findEmployeeIdsWithFacts(@Param("comId") Long comId,
                                       @Param("empIds") Collection<Long> empIds);
}
