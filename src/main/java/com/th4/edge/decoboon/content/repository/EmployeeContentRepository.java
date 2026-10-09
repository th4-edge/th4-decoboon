package com.th4.edge.decoboon.content.repository;

import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** 员工内容执行仓储。 */
public interface EmployeeContentRepository extends JpaRepository<EmployeeContent, Long> {
    Optional<EmployeeContent> findByTask_TaskIdAndEmployee_EmpId(Long taskId, Long empId);
    Optional<EmployeeContent> findByContentIdAndCompany_ComId(Long contentId, Long comId);
    List<EmployeeContent> findByCompany_ComIdAndEmployee_EmpIdOrderByContentIdDesc(Long comId, Long empId);
    List<EmployeeContent> findByCompany_ComIdOrderByContentIdDesc(Long comId);
    List<EmployeeContent> findByTask_TaskIdOrderByContentIdAsc(Long taskId);
    long countByCompany_ComIdAndStatus(Long comId, com.th4.edge.decoboon.content.configure.EmployeeContentStatus status);
}
