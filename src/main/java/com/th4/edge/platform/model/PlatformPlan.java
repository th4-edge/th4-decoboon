package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

/** 平台套餐只读快照。 */
@Note("平台套餐快照")
@Model
public class PlatformPlan {
    @Note("套餐ID") private Long planId;
    @Note("套餐编码") private String code;
    @Note("套餐名称") private String name;
    @Note("有效天数") private Integer days;
    @Note("最大门店") private Integer maxStores;
    @Note("最大席位") private Integer maxSeats;
    @Note("功能模块") private String modules;
    @Note("销售价") private Long price;
    @Note("原价") private Long originalPrice;
    @Note("说明") private String description;
    @Note("营销短句") private String tagline;
    @Note("权益亮点") private String highlights;
    @Note("角标") private String badge;
    @Note("推荐") private Boolean recommended;
    @Note("排序") private Integer priority;
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Integer getMaxStores() { return maxStores; }
    public void setMaxStores(Integer maxStores) { this.maxStores = maxStores; }
    public Integer getMaxSeats() { return maxSeats; }
    public void setMaxSeats(Integer maxSeats) { this.maxSeats = maxSeats; }
    public String getModules() { return modules; }
    public void setModules(String modules) { this.modules = modules; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Long originalPrice) { this.originalPrice = originalPrice; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public Boolean getRecommended() { return recommended; }
    public void setRecommended(Boolean recommended) { this.recommended = recommended; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
