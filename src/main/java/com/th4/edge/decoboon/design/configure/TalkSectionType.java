package com.th4.edge.decoboon.design.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 谈单 PPT 标准页型。 */
@Note("谈单PPT页型")
public enum TalkSectionType implements Enumerable<String> {
    Cover("封面"),
    Catalog("设计目录"),
    Community("小区景观"),
    Requirement("装修诉求"),
    SitePhoto("现场照片"),
    Survey("现场勘测"),
    SurveyReport("量房质量报告"),
    OriginalPlan("原始结构图"),
    Problem("户型问题"),
    Demolition("拆改图"),
    Masonry("砌筑图"),
    Optimization("优化图"),
    RoomDesign("区域设计"),
    SoftDecor("软装设计"),
    Material("材料搭配"),
    Appliance("电器配置"),
    Investment("装修投入"),
    Saving("省钱方案"),
    End("结束页");

    private final String name;

    TalkSectionType(String name) {
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
