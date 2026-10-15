package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 作业单行来源。
 *
 * <p>DEC-072 第 6 项：合同外/标配外主材必须先走增减项，确认生成材料需求后再下单。
 * 因此行来源**只有 {@link #Plan}**；"标配"是行的属性（{@code standardIncluded}），不是行来源。</p>
 */
@Note("作业单行来源")
public enum SheetItemSource implements Enumerable<String> {

    Plan("材料计划行（合同选材确认 / 增减项新增）");

    private final String name;

    SheetItemSource(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
