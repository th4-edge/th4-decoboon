package com.th4.edge.decoboon.catalog.configure;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.enums.Enumerable;

/** 报价项目库导入批次状态（PRD §7 冻结）。 */
@Note("报价项目库导入批次状态")
public enum CatalogImportStatus implements Enumerable<String> {

    Uploaded("文件已上传"),
    Parsed("已解析"),
    PendingConfirm("待确认"),
    Importing("导入中"),
    Imported("已导入"),
    PartiallyImported("部分导入"),
    Failed("导入失败"),
    Expired("已过期"),
    Revoked("已撤销");

    private final String name;

    CatalogImportStatus(String name) {
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

    /** 已写入正式项目库（可撤销）。 */
    public boolean imported() {
        return this == Imported || this == PartiallyImported;
    }

    /** 可作为重复文件命中的有效批次。 */
    public boolean active() {
        return this == Uploaded || this == Parsed || this == PendingConfirm || this == Importing || imported();
    }
}
