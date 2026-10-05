package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 报价内容修订令牌（并发冲突检测用）。
 *
 * 设计（无迁移）：
 * - 以**规范化内容指纹**作为不透明令牌，覆盖所有会影响正式报价的可编辑字段；
 * - 字段按稳定顺序拼接（行按 itemId 排序，字段固定顺序，金额统一 scale）；
 * - 输出 SHA-256 十六进制串。
 *
 * 为什么不用 updateTime：`DateTime` 只到秒，同一秒内的两次写入无法区分。
 * 内容指纹对「金额相同但 quantity/price/formula/space 变化」同样敏感。
 */
@Component
public class QuoteRevisionContext {

    private static final char SEP = '\u001F';
    private static final char ROW = '\u001E';

    public String token(Quote quote, List<QuoteLine> lines) {
        StringBuilder sb = new StringBuilder(256);
        append(sb, quote.getQuoteId());
        append(sb, quote.getStatus() == null ? null : quote.getStatus().name());
        append(sb, money(quote.getAmount()));
        append(sb, money(quote.getDiscount()));
        append(sb, money(quote.getTaxRate()));
        append(sb, money(quote.getTaxAmount()));
        append(sb, lines.size());
        append(sb, quote.getUpdateTime() == null ? null : quote.getUpdateTime().getDateTime());
        sb.append(ROW);

        lines.stream()
                .sorted(Comparator.comparing(QuoteLine::getItemId, Comparator.nullsLast(Long::compareTo)))
                .forEach(line -> {
                    append(sb, line.getItemId());
                    append(sb, quantity(line.getQuantity()));
                    append(sb, line.getQuantityFormula());
                    append(sb, money(line.getMaterialPrice()));
                    append(sb, money(line.getManualPrice()));
                    append(sb, money(line.getFixedTotalPrice()));
                    append(sb, line.getPricingMode() == null ? null : line.getPricingMode().name());
                    append(sb, line.getSpace() == null ? null : line.getSpace().getSpaceId());
                    append(sb, money(line.getDiscountAmount()));
                    append(sb, line.getIncluded());
                    append(sb, money(line.getAmount()));
                    sb.append(ROW);
                });

        return sha256(sb.toString());
    }

    private void append(StringBuilder sb, Object value) {
        sb.append(value == null ? "" : String.valueOf(value)).append(SEP);
    }

    /** 金额统一按 2 位小数归一，避免 1 与 1.00 产生不同指纹。 */
    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** 工程量按 3 位小数归一。 */
    private String quantity(BigDecimal value) {
        return value == null ? "" : value.setScale(3, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
