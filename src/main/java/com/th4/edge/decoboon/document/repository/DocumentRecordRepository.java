package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {
    Optional<DocumentRecord> findByDocumentIdAndCompany_ComId(Long documentId, Long comId);

    List<DocumentRecord> findByCompany_ComIdAndQuote_QuoteIdOrderByDocumentIdDesc(Long comId, Long quoteId);

    long countByCompany_ComIdAndQuote_QuoteId(Long comId, Long quoteId);

    List<DocumentRecord> findByCompany_ComIdAndTypeAndBusinessNoOrderByDocumentIdDesc(
            Long comId, com.th4.edge.decoboon.document.configure.DocumentType type, String businessNo);
}
