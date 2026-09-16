package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

/** 运营收入趋势点。 */
@Note("运营收入趋势")
@Model
public class RevenueTrend {
    @Note("月份")
    private String month;
    @Note("订单收入")
    private Long orderAmount;
    @Note("新增企业")
    private Long newCompanies;
    @Note("续费企业")
    private Long renewals;

    public RevenueTrend() {}
    public RevenueTrend(String month, Long orderAmount, Long newCompanies, Long renewals) {
        this.month = month;
        this.orderAmount = orderAmount;
        this.newCompanies = newCompanies;
        this.renewals = renewals;
    }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public Long getOrderAmount() { return orderAmount; }
    public void setOrderAmount(Long orderAmount) { this.orderAmount = orderAmount; }
    public Long getNewCompanies() { return newCompanies; }
    public void setNewCompanies(Long newCompanies) { this.newCompanies = newCompanies; }
    public Long getRenewals() { return renewals; }
    public void setRenewals(Long renewals) { this.renewals = renewals; }
}
