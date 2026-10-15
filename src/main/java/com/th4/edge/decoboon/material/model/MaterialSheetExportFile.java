package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

/**
 * 主材下单作业单导出文件（Excel 单据，BASE64 承载）。
 *
 * <p>导出内容按**导出时的单据快照**生成：模板或作业单后续变更不会改变已导出的文件。
 * 作业单对厂家，因此导出**不含价格、账期与成本**（DEC-073）。</p>
 */
@Note("主材下单作业单导出文件")
public class MaterialSheetExportFile {

    @Note("文件名")
    private String fileName;
    @Note("内容类型")
    private String contentType;
    @Note("文件内容（BASE64）")
    private String contentBase64;
    @Note("内容 SHA-256（与上传/归档对账用）")
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
