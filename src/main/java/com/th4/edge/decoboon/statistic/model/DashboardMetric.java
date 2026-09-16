package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/** 驾驶舱单指标及其目标、同比和环比。 */
@Note("驾驶舱指标")
public class DashboardMetric {

    private BigDecimal value = BigDecimal.ZERO;
    private BigDecimal target = BigDecimal.ZERO;
    private BigDecimal achievementRate = BigDecimal.ZERO;
    private BigDecimal previousMonthValue = BigDecimal.ZERO;
    private BigDecimal previousYearValue = BigDecimal.ZERO;
    private BigDecimal monthOverMonthRate = BigDecimal.ZERO;
    private BigDecimal yearOverYearRate = BigDecimal.ZERO;

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getTarget() { return target; }
    public void setTarget(BigDecimal target) { this.target = target; }
    public BigDecimal getAchievementRate() { return achievementRate; }
    public void setAchievementRate(BigDecimal achievementRate) { this.achievementRate = achievementRate; }
    public BigDecimal getPreviousMonthValue() { return previousMonthValue; }
    public void setPreviousMonthValue(BigDecimal previousMonthValue) { this.previousMonthValue = previousMonthValue; }
    public BigDecimal getPreviousYearValue() { return previousYearValue; }
    public void setPreviousYearValue(BigDecimal previousYearValue) { this.previousYearValue = previousYearValue; }
    public BigDecimal getMonthOverMonthRate() { return monthOverMonthRate; }
    public void setMonthOverMonthRate(BigDecimal monthOverMonthRate) { this.monthOverMonthRate = monthOverMonthRate; }
    public BigDecimal getYearOverYearRate() { return yearOverYearRate; }
    public void setYearOverYearRate(BigDecimal yearOverYearRate) { this.yearOverYearRate = yearOverYearRate; }
}
