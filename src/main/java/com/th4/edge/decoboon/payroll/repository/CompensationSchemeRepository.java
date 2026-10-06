package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface CompensationSchemeRepository extends JpaRepository<CompensationScheme, Long>, JpaSpecificationExecutor<CompensationScheme> {
    Optional<CompensationScheme> findByCompany_ComIdAndEmployee_EmpIdAndStatus(Long comId, Long empId, com.th4.edge.decoboon.payroll.configure.CompensationStatus status);
    List<CompensationScheme> findByCompany_ComIdAndEmployeeOrderByEffectiveFromDesc(Long comId, Employee employee);
}
