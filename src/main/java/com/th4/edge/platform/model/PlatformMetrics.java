package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 平台运营总览指标。 */
@Note("平台运营总览")
@Description("面向市场、运营和客户成功人员的跨企业只读经营快照")
@Model
public class PlatformMetrics {
    @Note("企业总数") @Unit("家") private Long totalCompanies = 0L;
    @Note("活跃订阅") @Unit("家") private Long activeSubscriptions = 0L;
    @Note("试用企业") @Unit("家") private Long trialSubscriptions = 0L;
    @Note("风险企业") @Unit("家") private Long riskCompanies = 0L;
    @Note("7日内到期") @Unit("家") private Long expiringSoon = 0L;
    @Note("本月新增") @Unit("家") private Long newCompaniesThisMonth = 0L;
    @Note("近30日订单金额") @Unit("分") private Long paidAmountLast30Days = 0L;
    @Note("月度经常性收入") @Unit("分") private Long mrr = 0L;
    @Note("席位使用率") @Unit("%") private Double seatUsageRate = 0D;
    @Note("门店使用率") @Unit("%") private Double storeUsageRate = 0D;
    @Note("订阅状态分布") private Map<String, Long> subscriptionStatus = new LinkedHashMap<>();
    @Note("套餐分布") private Map<String, Long> planDistribution = new LinkedHashMap<>();
    @Note("经营漏斗") private List<FunnelMetric> funnel = new ArrayList<>();
    @Note("近六月趋势") private List<RevenueTrend> revenueTrend = new ArrayList<>();

    public Long getTotalCompanies() { return totalCompanies; }
    public void setTotalCompanies(Long totalCompanies) { this.totalCompanies = totalCompanies; }
    public Long getActiveSubscriptions() { return activeSubscriptions; }
    public void setActiveSubscriptions(Long activeSubscriptions) { this.activeSubscriptions = activeSubscriptions; }
    public Long getTrialSubscriptions() { return trialSubscriptions; }
    public void setTrialSubscriptions(Long trialSubscriptions) { this.trialSubscriptions = trialSubscriptions; }
    public Long getRiskCompanies() { return riskCompanies; }
    public void setRiskCompanies(Long riskCompanies) { this.riskCompanies = riskCompanies; }
    public Long getExpiringSoon() { return expiringSoon; }
    public void setExpiringSoon(Long expiringSoon) { this.expiringSoon = expiringSoon; }
    public Long getNewCompaniesThisMonth() { return newCompaniesThisMonth; }
    public void setNewCompaniesThisMonth(Long newCompaniesThisMonth) { this.newCompaniesThisMonth = newCompaniesThisMonth; }
    public Long getPaidAmountLast30Days() { return paidAmountLast30Days; }
    public void setPaidAmountLast30Days(Long paidAmountLast30Days) { this.paidAmountLast30Days = paidAmountLast30Days; }
    public Long getMrr() { return mrr; }
    public void setMrr(Long mrr) { this.mrr = mrr; }
    public Double getSeatUsageRate() { return seatUsageRate; }
    public void setSeatUsageRate(Double seatUsageRate) { this.seatUsageRate = seatUsageRate; }
    public Double getStoreUsageRate() { return storeUsageRate; }
    public void setStoreUsageRate(Double storeUsageRate) { this.storeUsageRate = storeUsageRate; }
    public Map<String, Long> getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(Map<String, Long> subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public Map<String, Long> getPlanDistribution() { return planDistribution; }
    public void setPlanDistribution(Map<String, Long> planDistribution) { this.planDistribution = planDistribution; }
    public List<FunnelMetric> getFunnel() { return funnel; }
    public void setFunnel(List<FunnelMetric> funnel) { this.funnel = funnel; }
    public List<RevenueTrend> getRevenueTrend() { return revenueTrend; }
    public void setRevenueTrend(List<RevenueTrend> revenueTrend) { this.revenueTrend = revenueTrend; }
}
