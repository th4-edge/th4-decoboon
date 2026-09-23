package com.th4.edge.decoboon.common.support;

import com.th4.edge.framework.data.date.model.DateTime;

/**
 * 框架 {@link DateTime} 的展示格式化。
 *
 * <p>历史坑：框架 {@code DateTime} 是 {@code @Embedded} 且**没有覆写 toString**，
 * 直接拼字符串会输出 {@code com.th4.edge.framework.data.date.model.DateTime@1a2b}；
 * 它的 {@code getDateTime()} 返回的是 <b>yyyyMMddHHmmss 的 14 位数字</b>，不是 epoch 毫秒
 * （曾有人按毫秒换算，得出 2612 年）。所有面向用户的投影都必须走这里。</p>
 */
public final class DateTimeText {

    private DateTimeText() {
    }

    /** 框架 DateTime → {@code yyyy-MM-dd HH:mm:ss}；空值返回 {@code —}。 */
    public static String format(DateTime value) {
        return value == null ? "—" : formatPacked(value.getDateTime());
    }

    /** 14 位 {@code yyyyMMddHHmmss} → {@code yyyy-MM-dd HH:mm:ss}；非 14 位原样返回。 */
    public static String formatPacked(Long raw) {
        if (raw == null) {
            return "—";
        }
        String text = String.valueOf(raw);
        if (text.length() != 14) {
            return text;
        }
        return text.substring(0, 4) + "-" + text.substring(4, 6) + "-" + text.substring(6, 8)
                + " " + text.substring(8, 10) + ":" + text.substring(10, 12) + ":" + text.substring(12, 14);
    }
}
