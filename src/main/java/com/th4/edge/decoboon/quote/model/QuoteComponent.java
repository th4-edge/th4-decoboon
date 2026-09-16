package com.th4.edge.decoboon.quote.model;

import com.th4.edge.decoboon.quote.configure.QuoteComponentType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

/** 报价行内部的价格与成本组件快照。 */
@Note("报价组件")
@Embeddable
public class QuoteComponent {

    @Note("组件类型")
    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", length = 20, nullable = false)
    private QuoteComponentType type = QuoteComponentType.Other;

    @Note("组件名称")
    @Column(name = "component_name", length = 128)
    private String name;

    @Note("销售金额")
    @Unit("元")
    @Column(name = "sale_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal saleAmount = BigDecimal.ZERO;

    @Note("成本金额")
    @Unit("元")
    @Column(name = "cost_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Note("计价说明")
    @Column(name = "formula", length = 256)
    private String formula;

    @Note("排序")
    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    public QuoteComponentType getType() { return type; }
    public void setType(QuoteComponentType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getSaleAmount() { return saleAmount; }
    public void setSaleAmount(BigDecimal saleAmount) { this.saleAmount = saleAmount; }
    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}
