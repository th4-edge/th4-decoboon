package com.th4.edge.decoboon.catalog.model;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.framework.core.annotation.Note;

import java.util.List;
import java.util.Map;

/** 材料库分级选择树。 */
@Note("材料库分类树")
public class CatalogTree {

    private ItemType type;
    private List<String> categories;
    private Map<String, List<String>> brands;
    private Map<String, List<String>> specs;
    private long total;

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public Map<String, List<String>> getBrands() { return brands; }
    public void setBrands(Map<String, List<String>> brands) { this.brands = brands; }
    public Map<String, List<String>> getSpecs() { return specs; }
    public void setSpecs(Map<String, List<String>> specs) { this.specs = specs; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
}
