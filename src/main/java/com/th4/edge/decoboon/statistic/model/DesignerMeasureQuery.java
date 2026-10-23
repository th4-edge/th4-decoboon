package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 设计师待量房任务查询。 */
@Note("设计师待量房任务查询")
public class DesignerMeasureQuery {

    @Note("客户、房屋或手机号关键词")
    private String keyword;

    @Note("任务状态 key")
    private String state;

    @Note("页码，从 1 开始")
    private Integer current;

    @Note("每页条数")
    private Integer size;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
