package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 客户跟进仓储。 */
public interface CustomerFollowRepository extends JpaRepository<CustomerFollow, Long> {

    Page<CustomerFollow> findByCustomer_CustIdOrderByCreateTime_DateTimeDesc(Long customerId, Pageable pageable);

    List<CustomerFollow> findByCompany_ComId(Long comId);

    List<CustomerFollow> findTop200ByCompany_ComIdOrderByFollowIdDesc(Long comId);

    List<CustomerFollow> findByCompany_ComIdAndEmp_EmpId(Long comId, Long empId);

    boolean existsByCustomer_CustId(Long customerId);

    /** 批量统计客户跟进次数，供客户列表装配分级时避免 N+1。 */
    @Query("select f.customer.custId as customerId, count(f) as total from CustomerFollow f "
            + "where f.company.comId = :comId and f.customer.custId in :customerIds "
            + "group by f.customer.custId")
    List<CustomerFollowCount> countGroupByCustomer(@Param("comId") Long comId,
                                                   @Param("customerIds") Collection<Long> customerIds);

    /** 跟进次数投影。 */
    interface CustomerFollowCount {
        Long getCustomerId();

        Long getTotal();
    }
}
