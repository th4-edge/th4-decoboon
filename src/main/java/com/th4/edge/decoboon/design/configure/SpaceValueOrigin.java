package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 方案区域取值的来源（{@code DEC-066}）。
 *
 * <p>量房测的是**房屋原始结构**（布局固定），而装修设计会破坏结构（砌墙隔出新卧室、砸墙打通客厅），
 * 因此方案区域与量房原始区域**可以不同**，且二者**不联动**：</p>
 *
 * <ul>
 *   <li>{@link #Measure}：新建方案/实例化时**继承**量房原始区域作为起点（复制实测值，一次性的）；</li>
 *   <li>{@link #Override}：设计师在方案内录入/调整（原始结构在方案里被改造或方案新增区域）；</li>
 *   <li>{@link #TemplateReference}：模版参考实测值打底，**待实测确认**，不得进入工程量与报价金额。</li>
 * </ul>
 */
@Note("方案区域值来源")
public enum SpaceValueOrigin implements Enumerable<String> {
    Measure("量房原始结构继承"),
    Override("方案内录入"),
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
