package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 设计师待量房任务查询。 */
@Note("设计师待量房任务查询")
public class DesignerMeasureQuery {

    @Note("客户、房屋或手机号关键词")
    private String keyword;

    @Note("任务状态 key")
    private String state;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
