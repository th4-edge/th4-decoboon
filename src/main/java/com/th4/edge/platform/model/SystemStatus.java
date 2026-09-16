package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;

import java.util.ArrayList;
import java.util.List;

/** 平台服务器运行状态。 */
@Note("服务器运行状态")
@Description("平台运维总览所需的 JVM、主机资源、数据库、缓存和运行时长快照")
@Model
public class SystemStatus {
    @Note("主机名称") private String hostName;
    @Note("操作系统") private String osName;
    @Note("Java版本") private String javaVersion;
    @Note("应用版本") private String appVersion;
    @Note("CPU核心") @Unit("核") private Integer cpuCores;
    @Note("CPU使用率") @Unit("%") private Double cpuUsage;
    @Note("总内存") @Unit("字节") private Long totalMemory;
    @Note("已用内存") @Unit("字节") private Long usedMemory;
    @Note("最大堆内存") @Unit("字节") private Long maxMemory;
    @Note("磁盘总量") @Unit("字节") private Long diskTotal;
    @Note("已用磁盘") @Unit("字节") private Long diskUsed;
    @Note("运行时长") @Unit("秒") private Long uptimeSeconds;
    @Note("系统负载") private Double systemLoad;
    @Note("资源趋势") private List<SystemMetricPoint> history = new ArrayList<>();
    @Note("依赖服务") private List<ServiceHealth> services = new ArrayList<>();
    @Note("当前告警") private List<String> alerts = new ArrayList<>();

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }
    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public Integer getCpuCores() { return cpuCores; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }
    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
    public Long getTotalMemory() { return totalMemory; }
    public void setTotalMemory(Long totalMemory) { this.totalMemory = totalMemory; }
    public Long getUsedMemory() { return usedMemory; }
    public void setUsedMemory(Long usedMemory) { this.usedMemory = usedMemory; }
    public Long getMaxMemory() { return maxMemory; }
    public void setMaxMemory(Long maxMemory) { this.maxMemory = maxMemory; }
    public Long getDiskTotal() { return diskTotal; }
    public void setDiskTotal(Long diskTotal) { this.diskTotal = diskTotal; }
    public Long getDiskUsed() { return diskUsed; }
    public void setDiskUsed(Long diskUsed) { this.diskUsed = diskUsed; }
    public Long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(Long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
    public Double getSystemLoad() { return systemLoad; }
    public void setSystemLoad(Double systemLoad) { this.systemLoad = systemLoad; }
    public List<SystemMetricPoint> getHistory() { return history; }
    public void setHistory(List<SystemMetricPoint> history) { this.history = history; }
    public List<ServiceHealth> getServices() { return services; }
    public void setServices(List<ServiceHealth> services) { this.services = services; }
    public List<String> getAlerts() { return alerts; }
    public void setAlerts(List<String> alerts) { this.alerts = alerts; }
}
