package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CommissionItemRepository extends JpaRepository<CommissionItem, Long>, JpaSpecificationExecutor<CommissionItem> {
    List<CommissionItem> findByCompany_ComIdAndEmployee_EmpIdOrderByCreateTime_DateTimeDesc(Long comId, Long empId);
    List<CommissionItem> findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(Long comId, Long projectId);
    boolean existsByCompany_ComIdAndEmployee_EmpIdAndStatusIn(Long comId, Long empId, Collection<com.th4.edge.decoboon.payroll.configure.CommissionStatus> statuses);
    boolean existsByCompany_ComIdAndEmployee_EmpId(Long comId, Long empId);
    List<CommissionItem> findByPayrollItem_ItemIdOrderByCommissionIdAsc(Long itemId);

    /** 分页后的批量详情读取：预取页面白名单关联，避免逐行懒加载。 */
    @EntityGraph(attributePaths = {"employee", "project"})
    List<CommissionItem> findByCommissionIdIn(Collection<Long> commissionIds);

    @Query("""
            select distinct item.employee.empId from CommissionItem item
            where item.company.comId = :comId and item.employee.empId in :empIds
            """)
    Set<Long> findEmployeeIdsWithFacts(@Param("comId") Long comId,
                                       @Param("empIds") Collection<Long> empIds);
}
