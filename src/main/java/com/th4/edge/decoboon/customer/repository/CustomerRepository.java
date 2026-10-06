package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 客户仓储。
 */
public interface CustomerRepository extends JpaRepository<Customer, Long>,
        JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByCustIdAndCompany_ComId(Long custId, Long comId);

    List<Customer> findByCompany_ComId(Long comId);

    long countByCompany_ComId(Long comId);

    List<Customer> findTop200ByCompany_ComIdOrderByCustIdDesc(Long comId);

    List<Customer> findByCompany_ComIdAndOwner_EmpId(Long comId, Long empId);
    List<Customer> findByCompany_ComIdAndDesigner_EmpId(Long comId, Long empId);
    Optional<Customer> findByCustIdAndCompany_ComIdAndDesigner_EmpId(Long custId, Long comId, Long empId);

    List<Customer> findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(Long comId, Long storeId);

    List<Customer> findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(Long comId, Long teamId);
}
