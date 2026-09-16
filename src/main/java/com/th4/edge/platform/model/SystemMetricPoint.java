package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

import java.time.LocalDateTime;

/** 服务器资源趋势点。 */
@Note("服务器资源趋势点")
@Model
public class SystemMetricPoint {
    @Note("采样时间") private LocalDateTime sampleTime;
    @Note("CPU使用率") private Double cpuUsage;
    @Note("内存使用率") private Double memoryUsage;
    @Note("磁盘使用率") private Double diskUsage;
    @Note("网络接收字节") private Long networkRecv;
    @Note("网络发送字节") private Long networkSent;

    public LocalDateTime getSampleTime() { return sampleTime; }
    public void setSampleTime(LocalDateTime sampleTime) { this.sampleTime = sampleTime; }
    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
    public Double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
    public Double getDiskUsage() { return diskUsage; }
    public void setDiskUsage(Double diskUsage) { this.diskUsage = diskUsage; }
    public Long getNetworkRecv() { return networkRecv; }
    public void setNetworkRecv(Long networkRecv) { this.networkRecv = networkRecv; }
    public Long getNetworkSent() { return networkSent; }
    public void setNetworkSent(Long networkSent) { this.networkSent = networkSent; }
}
