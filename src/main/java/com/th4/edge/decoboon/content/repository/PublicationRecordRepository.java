package com.th4.edge.decoboon.content.repository;

import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

/** 内容发布记录仓储。 */
public interface PublicationRecordRepository extends JpaRepository<PublicationRecord, Long> {
    Optional<PublicationRecord> findFirstByEmployeeContent_ContentIdOrderByPublicationIdDesc(Long contentId);
    List<PublicationRecord> findByCompany_ComIdAndEmployee_EmpIdOrderByPublicationIdDesc(Long comId, Long empId);
    long countByCompany_ComIdAndStatus(Long comId, String status);
}
