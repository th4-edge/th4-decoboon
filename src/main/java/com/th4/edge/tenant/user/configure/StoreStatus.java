package com.th4.edge.tenant.user.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/**
 * 门店状态（`P0D-04` / `DESIGN-BIZ-20261013-002` §2）。
 *
 * <p>命名沿用既有 {@link EmployeeStatus} / {@code UserStatus} 风格。停用门店**不删除数据**：
 * 历史单据门店归属保留、仍计入「全部门店」合计并在控件中标注，但**不可作为新建归属目标**。</p>
 */
@Note("门店状态")
public enum StoreStatus implements Enumerable<String> {
    Active("启用"),
    Disabled("已停用");

    private final String name;

    StoreStatus(String name) { this.name = name; }
    @Override public String getName() { return name; }

    /** 是否可用于新建归属（只有启用门店可以承接新的客户/项目/单据/员工归属）。 */
    public boolean attributable() { return this == Active; }
}
