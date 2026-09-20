package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-风格偏好。 */
@Note("画像-风格偏好")
public enum PortraitStyle implements Enumerable<String> {

    Modern("现代简约"),
    Luxury("轻奢"),
    NewChinese("新中式"),
    Cream("奶油风"),
    Wood("原木"),
    Industrial("工业"),
    Undecided("待定");

    private final String name;

    PortraitStyle(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
