package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 客户跟进仓储。 */
public interface CustomerFollowRepository extends JpaRepository<CustomerFollow, Long> {

    Page<CustomerFollow> findByCustomer_CustIdOrderByCreateTime_DateTimeDesc(Long customerId, Pageable pageable);

    /** 跟进展示页：预取跟进人和客户，避免 OSIV 关闭后序列化懒加载失败。 */
    @EntityGraph(attributePaths = {"emp", "customer"})
    @Query("select f from CustomerFollow f where f.customer.custId = :customerId "
            + "order by f.createTime.dateTime desc")
    Page<CustomerFollow> findPageForDisplay(@Param("customerId") Long customerId, Pageable pageable);

    /** 批量预取当前页跟进的图片集合，避免逐条懒加载。 */
    @Query("select distinct f from CustomerFollow f "
            + "left join fetch f.images "
            + "left join fetch f.emp "
            + "left join fetch f.customer "
            + "where f.followId in :followIds")
    List<CustomerFollow> findWithImagesByFollowIds(@Param("followIds") Collection<Long> followIds);

    Optional<CustomerFollow> findByCompany_ComIdAndRequestId(Long comId, String requestId);

    /** 判断某个 OSS 对象键是否已经被其他跟进引用。 */
    @Query("select count(f) > 0 from CustomerFollow f join f.images i where i = :path")
    boolean existsByImagePath(@Param("path") String path);

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
