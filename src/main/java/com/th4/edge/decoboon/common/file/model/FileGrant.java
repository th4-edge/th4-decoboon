package com.th4.edge.decoboon.common.file.model;

import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

import java.time.LocalDateTime;

/** 短期私有文件访问凭证。 */
@Note("私有文件访问凭证")
@Model
public class FileGrant {

    private FileScope businessType;
    private Long businessId;
    private Long projectId;
    private String path;
    private String url;
    private LocalDateTime expiresAt;
    private String name;
    private String contentType;
    private Long size;

    public FileScope getBusinessType() { return businessType; }
    public void setBusinessType(FileScope businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
