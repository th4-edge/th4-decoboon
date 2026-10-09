package com.th4.edge.decoboon.material.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 主材选材记录状态。
 *
 * <p>DEC-056：设计师现场/逐项沟通与客户确认后代录，保存即"已选材（现场确认）"；
 * 不再区分草稿/待客户确认/已确认。进入采购前可继续修改，进入采购后由材料计划门禁锁定。</p>
 */
@Note("主材选材状态")
public enum SelectionStatus implements Enumerable<String> {

    Selected("已选材");

    private final String name;

    SelectionStatus(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /** 选材记录本身不再构成锁定；是否可改由材料计划状态（selectionLocked）判定。 */
    public boolean locked() {
        return false;
    }
}
