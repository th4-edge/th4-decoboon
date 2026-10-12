package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 方案区域取值的来源。
 *
 * <p>{@code DEC-064}（{@code EV-222}）：区域实测面积/周长的事实源是**房屋区域实测档案**，
 * 方案区域只是本次设计的取值：</p>
 *
 * <ul>
 *   <li>{@link #House}：继承房屋区域实测档案（可作工程量基数，{@code areaSource=Measured}）；</li>
 *   <li>{@link #Override}：设计师在方案内覆盖房屋值（现场差异，留痕；同样可作基数）；</li>
 *   <li>{@link #TemplateReference}：模版参考实测值打底，**待实测确认**，
 *       不承载房屋实测事实、不得进入工程量与报价金额（{@code areaSource=Estimated}）。</li>
 * </ul>
 */
@Note("方案区域值来源")
public enum SpaceValueOrigin implements Enumerable<String> {
    House("房屋档案继承"),
    Override("方案覆盖"),
    TemplateReference("模版参考值");

    private final String name;

    SpaceValueOrigin(String name) {
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
