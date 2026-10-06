package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Collection;
import java.util.List;

public interface CommissionItemRepository extends JpaRepository<CommissionItem, Long>, JpaSpecificationExecutor<CommissionItem> {
    List<CommissionItem> findByCompany_ComIdAndEmployee_EmpIdOrderByCreateTime_DateTimeDesc(Long comId, Long empId);
    List<CommissionItem> findByCompany_ComIdAndProject_ProjectIdOrderByCreateTime_DateTimeDesc(Long comId, Long projectId);
    boolean existsByCompany_ComIdAndEmployee_EmpIdAndStatusIn(Long comId, Long empId, Collection<com.th4.edge.decoboon.payroll.configure.CommissionStatus> statuses);
    List<CommissionItem> findByPayrollItem_ItemIdOrderByCommissionIdAsc(Long itemId);
}
