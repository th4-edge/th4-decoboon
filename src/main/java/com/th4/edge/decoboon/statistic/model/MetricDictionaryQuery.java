package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.List;

/** 指标口径字典查询。 */
@Note("指标口径字典查询")
public class MetricDictionaryQuery {

    @Note("分类筛选：finance/operation/period；为空返回全部")
    private String category;

    @Note("指标 key 白名单；为空返回全部")
    private List<String> keys;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getKeys() { return keys; }
    public void setKeys(List<String> keys) { this.keys = keys; }
}
