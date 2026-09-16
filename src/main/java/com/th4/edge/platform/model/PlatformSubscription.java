package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;

/** 平台订阅只读快照。 */
@Note("平台订阅快照")
@Model
public class PlatformSubscription {
    @Note("订阅ID") private Long subId;
    @Note("企业ID") private Long comId;
    @Note("企业名称") private String companyTitle;
    @Note("套餐编码") private String planCode;
    @Note("套餐名称") private String planName;
    @Note("订阅状态") private String status;
    @Note("服务到期时间") private DateTime serviceEndTime;
    @Note("剩余天数") private Integer remainingDays;
    @Note("加购席位") private Integer extraSeats;
    @Note("加购门店") private Integer extraStores;
    @Note("已用席位") private Long usedSeats;
    @Note("最大席位") private Integer effectiveMaxSeats;
    @Note("已用门店") private Long usedStores;
    @Note("最大门店") private Integer effectiveMaxStores;
    @Note("是否可写") private Boolean writable;
    @Note("是否过期") private Boolean expired;
    public Long getSubId() { return subId; }
    public void setSubId(Long subId) { this.subId = subId; }
    public Long getComId() { return comId; }
    public void setComId(Long comId) { this.comId = comId; }
    public String getCompanyTitle() { return companyTitle; }
    public void setCompanyTitle(String companyTitle) { this.companyTitle = companyTitle; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public DateTime getServiceEndTime() { return serviceEndTime; }
    public void setServiceEndTime(DateTime serviceEndTime) { this.serviceEndTime = serviceEndTime; }
    public Integer getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Integer remainingDays) { this.remainingDays = remainingDays; }
    public Integer getExtraSeats() { return extraSeats; }
    public void setExtraSeats(Integer extraSeats) { this.extraSeats = extraSeats; }
    public Integer getExtraStores() { return extraStores; }
    public void setExtraStores(Integer extraStores) { this.extraStores = extraStores; }
    public Long getUsedSeats() { return usedSeats; }
    public void setUsedSeats(Long usedSeats) { this.usedSeats = usedSeats; }
    public Integer getEffectiveMaxSeats() { return effectiveMaxSeats; }
    public void setEffectiveMaxSeats(Integer effectiveMaxSeats) { this.effectiveMaxSeats = effectiveMaxSeats; }
    public Long getUsedStores() { return usedStores; }
    public void setUsedStores(Long usedStores) { this.usedStores = usedStores; }
    public Integer getEffectiveMaxStores() { return effectiveMaxStores; }
    public void setEffectiveMaxStores(Integer effectiveMaxStores) { this.effectiveMaxStores = effectiveMaxStores; }
    public Boolean getWritable() { return writable; }
    public void setWritable(Boolean writable) { this.writable = writable; }
    public Boolean getExpired() { return expired; }
    public void setExpired(Boolean expired) { this.expired = expired; }
}
