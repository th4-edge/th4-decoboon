package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

@Note("业务员或门店排名")
public class MarketerRankingItem {
    private Long id;
    private String name;
    private Integer rank = 0;
    private Integer customerCount = 0;
    private Integer measureCount = 0;
    private Integer followCount = 0;
    private BigDecimal measureRate = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getMeasureCount() { return measureCount; }
    public void setMeasureCount(Integer measureCount) { this.measureCount = measureCount; }
    public Integer getFollowCount() { return followCount; }
    public void setFollowCount(Integer followCount) { this.followCount = followCount; }
    public BigDecimal getMeasureRate() { return measureRate; }
    public void setMeasureRate(BigDecimal measureRate) { this.measureRate = measureRate; }
}
