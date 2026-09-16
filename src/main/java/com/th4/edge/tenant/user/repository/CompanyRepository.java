package com.th4.edge.tenant.user.repository;

import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.billing.configure.SubStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 企业仓储。
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Company findByComId(Long comId);

    Company findFirstByTitle(String title);

    boolean existsByTitle(String title);

    List<Company> findByPlatformFalseOrderByComIdDesc();

    long countByPlatformFalse();

    long countByPlatformFalseAndCreateTime_DateTimeGreaterThanEqual(com.th4.edge.framework.data.date.model.DateTime time);

    @Query("""
            select c from Company c
            where c.platform = false
              and (:keyword is null
                   or lower(c.title) like lower(concat('%', :keyword, '%'))
                   or lower(c.name) like lower(concat('%', :keyword, '%')))
              and (:status is null
                   or exists (select s from Subscription s where s.company = c and s.status = :status))
            order by c.comId desc
            """)
    Page<Company> pagePlatformTenants(@Param("keyword") String keyword,
                                      @Param("status") SubStatus status,
                                      Pageable pageable);
}
