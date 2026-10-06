package com.th4.edge.decoboon.finance.context;

import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 收付款客户端提交意图与业务指纹的公共校验逻辑。 */
@Component
public class FinanceIdempotencyContext {

    public static final String CONFLICT_MESSAGE = "该幂等键已被其他收付款占用，请勿复用";

    /** 客户端传入非空键时原样使用；兼容旧客户端时退回确定性业务键。 */
    public String clientKeyOr(String clientKey, String fallback) {
        if (clientKey != null && !clientKey.isBlank()) {
            return clientKey.trim();
        }
        return fallback;
    }

    /** 业务指纹统一使用 SHA-256，避免把金额、应付列表等业务字段直接暴露在索引列中。 */
    public String fingerprint(String value) {
        return sha256(value);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    public String amount(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    public String text(String value) {
        return value == null ? "" : value.trim();
    }

    public DefinedException conflict() {
        return new DefinedException(new DefaultErrorCode("409", CONFLICT_MESSAGE));
    }

    /** H2 报 unique violation，MySQL 报 Duplicate entry；仅把唯一键冲突翻译成 409。 */
    public boolean isDuplicateKey(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("duplicate entry")
                        || normalized.contains("unique index")
                        || normalized.contains("unique constraint")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
