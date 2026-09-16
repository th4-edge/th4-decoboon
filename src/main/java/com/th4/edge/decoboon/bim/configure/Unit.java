package com.th4.edge.decoboon.bim.configure;

import com.th4.edge.framework.data.enums.Enumerable;

/***
 *  m²
 *  个
 *  项
 *  处
 *  套
 *  户
 *  米
 *  步
 *  樘
 *  块
 *  根
 *  扇
 *  台
 *  支
 *  格
 */
public enum Unit implements Enumerable<String> {
    Area("m²"),
    Count("个"),
    Item("项"),
    Place("处"),
    Set("套"),
    House("户"),
    Metre("m"),
    Centimeter("cm"),
    Step("步"),
    Plate("樘"),
    Block("块"),

    Root("根"),
    Hole("扇"),
    Pcs("台"),
    Stick("支"),
    Grid("格"),
    Sheet("片")
    ;

    private final String name;

    Unit(String name) {
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
