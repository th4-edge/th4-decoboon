package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

/** 依赖服务健康状态。 */
@Note("依赖服务健康")
@Model
public class ServiceHealth {
    @Note("服务名称") private String name;
    @Note("状态") private String status;
    @Note("延迟毫秒") private Long latencyMs;
    @Note("说明") private String detail;

    public ServiceHealth() {}
    public ServiceHealth(String name, String status, Long latencyMs, String detail) {
        this.name = name;
        this.status = status;
        this.latencyMs = latencyMs;
        this.detail = detail;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
