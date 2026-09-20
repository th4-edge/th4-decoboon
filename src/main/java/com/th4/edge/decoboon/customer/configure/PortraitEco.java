package com.th4.edge.decoboon.customer.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 画像-环保健康要求。 */
@Note("画像-环保健康要求")
public enum PortraitEco implements Enumerable<String> {

    Elder("有老人"),
    Infant("有婴幼儿"),
    Pregnant("有孕妇"),
    None("无特殊要求");

    private final String name;

    PortraitEco(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
