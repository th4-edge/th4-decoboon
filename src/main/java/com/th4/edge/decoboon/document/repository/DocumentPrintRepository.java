package com.th4.edge.decoboon.document.repository;

import com.th4.edge.decoboon.document.model.DocumentPrint;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

public interface DocumentPrintRepository extends JpaRepository<DocumentPrint, Long> {
    long countByDocument_DocumentId(Long documentId);
}
