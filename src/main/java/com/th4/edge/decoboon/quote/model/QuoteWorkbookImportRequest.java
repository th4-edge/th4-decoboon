package com.th4.edge.decoboon.quote.model;

import com.th4.edge.framework.core.annotation.Note;

@Note("报价工作簿导入请求")
public class QuoteWorkbookImportRequest {
    private Long quoteId;
    private String fileName;
    private String contentBase64;

    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentBase64() { return contentBase64; }
    public void setContentBase64(String contentBase64) { this.contentBase64 = contentBase64; }
}
