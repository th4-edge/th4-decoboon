package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 员工仓储。
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Employee findByEmpId(Long empId);

    Employee findByEmpIdAndCompany_ComId(Long empId, Long comId);

    Employee findByTelNo(String telNo);

    Employee findFirstByTelNoOrderByEmpIdDesc(String telNo);


    Employee findFirstByTelNoAndStatusOrderByEmpIdDesc(String telNo, EmployeeStatus status);


    long countByCompany_ComIdAndStatus(Long comId, EmployeeStatus status);

    long countByCompany_ComIdAndRoleAndStatus(Long comId, EmployeeRole role, EmployeeStatus status);

    long countByCompany_ComId(Long comId);

    List<Employee> findByCompany_ComId(Long comId);

    @EntityGraph(attributePaths = {"store", "team"})
    List<Employee> findByCompany_ComIdOrderByNameAsc(Long comId);

    @EntityGraph(attributePaths = {"store", "team"})
    List<Employee> findByCompany_ComIdAndStatusOrderByNameAsc(Long comId, EmployeeStatus status);

    List<Employee> findByCompany_ComIdInAndRoleOrderByNameAsc(Collection<Long> comIds, EmployeeRole role);

    Page<Employee> findByCompany_ComId(Long comId, Pageable pageable);

    Page<Employee> findByCompany_ComIdAndStore_StoreId(Long comId, Long storeId, Pageable pageable);

    Page<Employee> findByCompany_ComIdAndTeam_TeamId(Long comId, Long teamId, Pageable pageable);

    List<Employee> findByCompany_ComIdAndStore_StoreIdOrderByName(Long comId, Long storeId);

    List<Employee> findByCompany_ComIdAndTeam_TeamIdOrderByName(Long comId, Long teamId);

    List<Employee> findByCompany_ComIdAndRoleOrderByName(Long comId, EmployeeRole role);

    List<Employee> findByCompany_ComIdAndRoleInOrderByName(Long comId, java.util.Collection<EmployeeRole> roles);

    List<Employee> findByCompany_ComIdAndStore_StoreIdAndRoleOrderByName(Long comId, Long storeId, EmployeeRole role);

    List<Employee> findByCompany_ComIdAndStore_StoreIdAndRoleInOrderByName(Long comId, Long storeId, java.util.Collection<EmployeeRole> roles);

    List<Employee> findByCompany_ComIdAndTeam_TeamIdAndRoleOrderByName(Long comId, Long teamId, EmployeeRole role);

    List<Employee> findByCompany_ComIdAndTeam_TeamIdAndRoleInOrderByName(Long comId, Long teamId, java.util.Collection<EmployeeRole> roles);

    /** 门店维度员工（`DEC-067`）：全部门店不过滤，单店按员工 `store` 收窄（SQL 内完成）。 */
    @Query("select e from Employee e left join e.store es where e.company.comId = :comId "
            + "and (:allStores = true or es.storeId = :storeId)")
    List<Employee> findByCompanyStoreScope(@Param("comId") Long comId,
                                           @Param("allStores") boolean allStores,
                                           @Param("storeId") Long storeId);

    /** 团队 + 门店维度（门店维度收窄时把门店条件一并下推 SQL）。 */
    List<Employee> findByCompany_ComIdAndTeam_TeamIdAndStore_StoreIdOrderByName(
            Long comId, Long teamId, Long storeId);
}
