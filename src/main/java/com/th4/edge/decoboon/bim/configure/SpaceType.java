package com.th4.edge.decoboon.bim.configure;

import com.th4.edge.framework.data.enums.Enumerable;
import com.th4.edge.framework.core.annotation.Note;

@Note("装修空间")
public enum SpaceType implements Enumerable<String> {
    Bedroom("卧室"),
    Kitchen("厨房"),
    LivingRoom("客厅"),
    DiningRoom("餐厅" ),
    Bathroom("卫生间"),
    Balcony("阳台");

    private final String name;

    SpaceType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public String getValue() {
        return toString();
    }
}
