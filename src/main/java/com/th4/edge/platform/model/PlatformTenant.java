package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;

/** 平台的租户只读快照，不持有任何租户实体引用。 */
@Note("平台租户快照")
@Description("由租户适配器按标量字段组装，数据库不建立平台与租户外键")
@Model
public class PlatformTenant {
    @Note("企业ID") private Long comId;
    @Note("企业名称") private String title;
    @Note("企业简称") private String name;
    @Note("Logo") private String logo;
    @Note("品牌色") private String color;
    @Note("规模") private String size;
    @Note("地址") private String addr;
    @Note("创建时间") private DateTime createTime;
    @Note("订阅ID") private Long subId;
    @Note("套餐名称") private String planName;
    @Note("订阅状态") private String subscriptionStatus;
    @Note("服务到期时间") private DateTime serviceEndTime;
    @Note("剩余天数") private Integer remainingDays;
    @Note("已用席位") private Long usedSeats;
    @Note("最大席位") private Integer effectiveMaxSeats;
    @Note("已用门店") private Long usedStores;
    @Note("最大门店") private Integer effectiveMaxStores;
    @Note("是否启用门店") private Boolean enableStore;

    public Long getComId() { return comId; }
    public void setComId(Long comId) { this.comId = comId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getAddr() { return addr; }
    public void setAddr(String addr) { this.addr = addr; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
    public Long getSubId() { return subId; }
    public void setSubId(Long subId) { this.subId = subId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public DateTime getServiceEndTime() { return serviceEndTime; }
    public void setServiceEndTime(DateTime serviceEndTime) { this.serviceEndTime = serviceEndTime; }
    public Integer getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Integer remainingDays) { this.remainingDays = remainingDays; }
    public Long getUsedSeats() { return usedSeats; }
    public void setUsedSeats(Long usedSeats) { this.usedSeats = usedSeats; }
    public Integer getEffectiveMaxSeats() { return effectiveMaxSeats; }
    public void setEffectiveMaxSeats(Integer effectiveMaxSeats) { this.effectiveMaxSeats = effectiveMaxSeats; }
    public Long getUsedStores() { return usedStores; }
    public void setUsedStores(Long usedStores) { this.usedStores = usedStores; }
    public Integer getEffectiveMaxStores() { return effectiveMaxStores; }
    public void setEffectiveMaxStores(Integer effectiveMaxStores) { this.effectiveMaxStores = effectiveMaxStores; }
    public Boolean getEnableStore() { return enableStore; }
    public void setEnableStore(Boolean enableStore) { this.enableStore = enableStore; }
}
