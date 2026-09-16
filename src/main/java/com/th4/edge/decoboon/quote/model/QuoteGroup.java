package com.th4.edge.decoboon.quote.model;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 报价的领域汇总视图，可用于区域结构或项目经理施工分项结构。 */
@Note("报价汇总")
public class QuoteGroup {

    private String key;
    private String name;
    private ItemType type;
    private String category;
    private Unit unit;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal cost = BigDecimal.ZERO;
    private List<QuoteLine> items = new ArrayList<>();

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public List<QuoteLine> getItems() { return items; }
    public void setItems(List<QuoteLine> items) { this.items = items; }
}
