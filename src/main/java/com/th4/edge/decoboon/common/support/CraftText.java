package com.th4.edge.decoboon.common.support;

/**
 * 施工工艺文本解析。
 *
 * <p>施工工艺的主数据在材料库（{@code CatalogItem.craft}），设计分项与报价行各自冻结一份快照。
 * 历史行可能没有快照值，读取时统一按"自身快照 → 上游主数据"的顺序兜底，避免同一业务事实出现两套口径。</p>
 */
public final class CraftText {

    private CraftText() {
    }

    /** 返回第一个非空白文本；全部为空时返回 null。 */
    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
