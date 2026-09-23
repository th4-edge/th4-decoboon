package com.th4.edge.decoboon.document.context;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额大写转换。
 *
 * <p>规则冻结于《凭证套打与单据模板字段冻结清单》第 12 节：
 * 整元用"整"、有角无分只到角、中间零按中文习惯合并、负数前置"负"。</p>
 */
public final class VoucherAmountWords {

    private static final char[] DIGITS = "零壹贰叁肆伍陆柒捌玖".toCharArray();
    private static final String[] UNITS = {"", "拾", "佰", "仟"};
    private static final String[] SECTIONS = {"", "万", "亿"};

    private VoucherAmountWords() {
    }

    public static String of(BigDecimal value) {
        if (value == null) return "";
        BigDecimal amount = value.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() == 0) return "零元整";
        boolean negative = amount.signum() < 0;
        amount = amount.abs();
        long yuan = amount.longValue();
        int cents = amount.remainder(BigDecimal.ONE).movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP).intValue();

        StringBuilder result = new StringBuilder();
        if (negative) result.append('负');
        result.append(integerWords(yuan)).append('元');
        int jiao = cents / 10;
        int fen = cents % 10;
        if (jiao == 0 && fen == 0) {
            result.append('整');
        } else {
            if (jiao > 0) {
                result.append(DIGITS[jiao]).append('角');
            } else if (yuan > 0) {
                result.append('零');
            }
            if (fen > 0) result.append(DIGITS[fen]).append('分');
        }
        return result.toString();
    }

    private static String integerWords(long number) {
        if (number == 0) return "零";
        StringBuilder result = new StringBuilder();
        int index = 0;
        boolean zeroPending = false;
        long rest = number;
        while (rest > 0 && index < SECTIONS.length) {
            int section = (int) (rest % 10000);
            if (section == 0) {
                if (result.length() > 0) zeroPending = true;
            } else {
                String text = sectionWords(section);
                if (zeroPending && result.length() > 0) result.insert(0, "零");
                result.insert(0, text + SECTIONS[index]);
                zeroPending = section < 1000;
            }
            rest /= 10000;
            index++;
        }
        return result.toString();
    }

    private static String sectionWords(int section) {
        StringBuilder builder = new StringBuilder();
        boolean zero = false;
        int unit = 0;
        int rest = section;
        while (rest > 0) {
            int digit = rest % 10;
            if (digit == 0) {
                if (builder.length() > 0) zero = true;
            } else {
                if (zero) {
                    builder.insert(0, '零');
                    zero = false;
                }
                builder.insert(0, DIGITS[digit] + UNITS[unit]);
            }
            rest /= 10;
            unit++;
        }
        return builder.toString();
    }
}
