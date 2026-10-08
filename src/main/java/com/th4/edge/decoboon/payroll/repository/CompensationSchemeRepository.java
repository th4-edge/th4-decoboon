package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompensationSchemeRepository extends JpaRepository<CompensationScheme, Long>, JpaSpecificationExecutor<CompensationScheme> {
    Optional<CompensationScheme> findByCompany_ComIdAndEmployee_EmpIdAndStatus(Long comId, Long empId, CompensationStatus status);
    List<CompensationScheme> findByCompany_ComIdAndEmployeeOrderByEffectiveFromDesc(Long comId, Employee employee);

    /** 按工资归属月份匹配方案，避免补发历史工资时使用当前方案。 */
    @Query("""
            select scheme from CompensationScheme scheme
            where scheme.company.comId = :comId
              and scheme.employee.empId = :empId
              and scheme.status = :status
              and scheme.effectiveFrom <= :periodEnd
              and (scheme.effectiveTo is null or scheme.effectiveTo >= :periodStart)
            order by scheme.effectiveFrom desc
            """)
    List<CompensationScheme> findEffective(@Param("comId") Long comId,
                                           @Param("empId") Long empId,
                                           @Param("status") CompensationStatus status,
                                           @Param("periodStart") LocalDate periodStart,
                                           @Param("periodEnd") LocalDate periodEnd);
}
