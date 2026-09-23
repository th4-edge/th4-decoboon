package com.th4.edge.decoboon.finance.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;

/** 应收/应付账龄分档。 */
@Note("账龄分档")
public class AgingBucket {

    @Note("分档键")
    private String key;

    @Note("分档名称")
    private String label;

    @Unit("元")
    @Note("分档金额")
    private BigDecimal amount = BigDecimal.ZERO;

    @Note("分档笔数")
    private Integer count = 0;

    @Note("展示色调：normal 正常 / warn 关注 / danger 严重 / unknown 待确认")
    private String level = "normal";

    public AgingBucket() { }

    public AgingBucket(String key, String label, String level) {
        this.key = key;
        this.label = label;
        this.level = level;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}
