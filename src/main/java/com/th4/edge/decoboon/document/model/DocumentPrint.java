package com.th4.edge.decoboon.document.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 单据生成、打印和重打轨迹。 */
@Note("单据打印记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_document_print", indexes = {
        @Index(name = "idx_document_print_company", columnList = "com_id, print_id"),
        @Index(name = "idx_document_print_document", columnList = "com_id, document_id, print_id")
})
public class DocumentPrint extends AuditModel {

    @Id @SnowflakeId @Note("打印记录索引") private Long printId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "com_id", nullable = false) @Note("所属企业") private Company company;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false) @Note("归档记录") private DocumentRecord document;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "printed_by_id", nullable = false) @Note("操作人") private Employee printedBy;
    @Column(nullable = false) @Note("打印序号") private Integer sequenceNo = 1;
    @Column(length = 128) @Note("重打原因") private String reason;
    @Column(nullable = false) @Note("操作时间") private LocalDateTime printedAt = LocalDateTime.now();

    public Long getPrintId() { return printId; }
    public void setPrintId(Long printId) { this.printId = printId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public DocumentRecord getDocument() { return document; }
    public void setDocument(DocumentRecord document) { this.document = document; }
    public Employee getPrintedBy() { return printedBy; }
    public void setPrintedBy(Employee printedBy) { this.printedBy = printedBy; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getPrintedAt() { return printedAt; }
    public void setPrintedAt(LocalDateTime printedAt) { this.printedAt = printedAt; }
}
