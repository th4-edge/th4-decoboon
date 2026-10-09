package com.th4.edge.decoboon.content.repository;
import com.th4.edge.decoboon.content.model.LeadTouch;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
/** 内容线索触点仓储。 */
public interface LeadTouchRepository extends JpaRepository<LeadTouch, Long> {
    Optional<LeadTouch> findFirstByCustomer_CustIdAndEmployeeContent_ContentId(Long customerId, Long contentId);
    List<LeadTouch> findByCustomer_CustIdOrderByTouchIdDesc(Long customerId);
    List<LeadTouch> findByCustomer_CustIdAndCompany_ComIdOrderByTouchIdDesc(Long customerId, Long comId);
    List<LeadTouch> findByCompany_ComIdOrderByTouchIdDesc(Long comId);
    List<LeadTouch> findByEmployeeContent_ContentIdOrderByTouchIdDesc(Long contentId);
}
