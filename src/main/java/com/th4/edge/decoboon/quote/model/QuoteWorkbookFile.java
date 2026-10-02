package com.th4.edge.decoboon.quote.model;

import com.th4.edge.framework.core.annotation.Note;

/** Web 报价工作簿导出文件。 */
@Note("报价工作簿文件")
public class QuoteWorkbookFile {
    private String fileName;
    private String contentType;
    private String contentBase64;
    private String sha256;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentBase64() { return contentBase64; }
    public void setContentBase64(String contentBase64) { this.contentBase64 = contentBase64; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
}
