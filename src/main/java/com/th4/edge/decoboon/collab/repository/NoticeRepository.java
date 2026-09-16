package com.th4.edge.decoboon.collab.repository;

import com.th4.edge.decoboon.collab.model.Notice;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findByReceiver_EmpId(Long empId, Pageable pageable);

    Page<Notice> findByReceiver_EmpIdAndReadFalse(Long empId, Pageable pageable);

    long countByReceiver_EmpIdAndReadFalse(Long empId);

    boolean existsByReceiver_EmpIdAndBizKey(Long empId, String bizKey);

    Optional<Notice> findByReceiver_EmpIdAndBizKey(Long empId, String bizKey);

    long deleteByReceiver_EmpIdAndQuoteIsNotNull(Long empId);

    Optional<Notice> findByNoticeIdAndReceiver_EmpId(Long noticeId, Long empId);
}
