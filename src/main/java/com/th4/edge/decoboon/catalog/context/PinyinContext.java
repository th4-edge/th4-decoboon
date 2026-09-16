package com.th4.edge.decoboon.catalog.context;

import net.sourceforge.pinyin4j.PinyinHelper;
import org.springframework.stereotype.Component;

/**
 * 中文拼音首字母生成。
 *
 * <p>用于报价项目名称的拼音检索：如「轻钢龙骨石膏板吊平顶」→ {@code QGLGSGBDPD}，
 * 使不熟悉完整名称的录入者可直接按首字母搜索。</p>
 */
@Component
public class PinyinContext {

    private static final int MAX_INITIALS = 64;

    /** 生成名称的拼音首字母串；非中文字符保留字母数字（大写），其余忽略。 */
    public String initials(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        StringBuilder buffer = new StringBuilder();
        for (char ch : text.toCharArray()) {
            String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(ch);
            if (pinyin != null && pinyin.length > 0 && !pinyin[0].isEmpty()) {
                buffer.append(Character.toUpperCase(pinyin[0].charAt(0)));
            } else if (Character.isLetterOrDigit(ch)) {
                buffer.append(Character.toUpperCase(ch));
            }
        }
        if (buffer.isEmpty()) {
            return null;
        }
        if (buffer.length() > MAX_INITIALS) {
            buffer.setLength(MAX_INITIALS);
        }
        return buffer.toString();
    }
}
