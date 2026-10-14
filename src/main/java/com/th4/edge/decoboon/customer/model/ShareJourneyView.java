package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 亲友只读视图（非表投影，沿用 CustomerJourney 先例）。
 *
 * 硬边界：
 * <ul>
 *   <li>不含客户姓名、电话、具体房号与金额（Friend 范围）；</li>
 *   <li>不含成本、利润、供应商与任何内部字段。</li>
 * </ul>
 */
@Note("亲友只读视图")
public class ShareJourneyView {

    @Note("项目名称") private String projectName;
    @Note("小区与楼栋（不含房号）") private String community;
    @Note("整体进度") private Integer progress = 0;
    @Note("当前阶段名称") private String currentStageName;
    @Note("现场照片") private List<String> photos = new ArrayList<>();
    @Note("服务方") private CustomerJourney.Vendor vendor = new CustomerJourney.Vendor();
    @Note("数据时间") private LocalDateTime asOf;

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getCurrentStageName() { return currentStageName; }
    public void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
    public CustomerJourney.Vendor getVendor() { return vendor; }
    public void setVendor(CustomerJourney.Vendor vendor) { this.vendor = vendor; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
}
