package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 标签分类。 */
@Note("标签分类")
public enum TagCategory implements Enumerable<String> {

    Source("来源"),
    Behavior("行为"),
    Intent("意向"),
    Demand("需求"),
    Preference("偏好"),
    Value("价值"),
    Risk("风险");

    private final String name;

    TagCategory(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
