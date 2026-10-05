package com.th4.edge.decoboon.carryover.model;

import com.th4.edge.framework.core.annotation.Note;

/** 双轨对账导出内容（P0 采用 UTF-8 CSV，可直接用 Excel 打开）。 */
@Note("双轨对账导出")
public class CarryoverReconciliationExport {

    private String fileName;
    private String contentType;
    private String content;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
