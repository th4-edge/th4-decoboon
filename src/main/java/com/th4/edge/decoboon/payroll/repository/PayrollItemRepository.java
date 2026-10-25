package com.th4.edge.decoboon.payroll.repository;

import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long>, JpaSpecificationExecutor<PayrollItem> {
    List<PayrollItem> findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(Long comId, Long sheetId);
    List<PayrollItem> findByCompany_ComIdAndEmployee_EmpIdAndSheet_PeriodOrderByItemIdAsc(Long comId, Long empId, String period);
    Optional<PayrollItem> findByCompany_ComIdAndSheet_SheetIdAndEmployee_EmpIdAndAttributionKey(Long comId, Long sheetId, Long empId, String attributionKey);
    boolean existsByCompany_ComIdAndEmployee_EmpId(Long comId, Long empId);
    boolean existsByCompany_ComIdAndEmployee_EmpIdAndSheet_Period(Long comId, Long empId, String period);

    @Query("""
            select distinct item.employee.empId from PayrollItem item
            where item.company.comId = :comId and item.employee.empId in :empIds
            """)
    Set<Long> findEmployeeIdsWithFacts(@Param("comId") Long comId,
                                       @Param("empIds") Collection<Long> empIds);

    /** 公司级工资单在单店视图下的可见性：工资单内至少有一个该门店的工资项。 */
    @Query("""
            select distinct item.sheet.sheetId from PayrollItem item
            where item.company.comId = :comId and item.store.storeId = :storeId
              and item.sheet is not null
            """)
    List<Long> findSheetIdsByStore(@Param("comId") Long comId, @Param("storeId") Long storeId);
}
