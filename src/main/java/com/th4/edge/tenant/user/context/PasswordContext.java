package com.th4.edge.tenant.user.context;

import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/** 密码哈希、策略校验和 bootstrap token 常量时间比较。 */
@Component
public class PasswordContext {

    private static final Pattern LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        validate(rawPassword);
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return rawPassword != null && passwordHash != null && encoder.matches(rawPassword, passwordHash);
    }

    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw new DefinedException(new DefaultErrorCode("PASSWORD_POLICY",
                    "密码长度必须为 8-64 位"));
        }
        if (!LETTER.matcher(rawPassword).matches() || !DIGIT.matcher(rawPassword).matches()) {
            throw new DefinedException(new DefaultErrorCode("PASSWORD_POLICY",
                    "密码必须同时包含字母和数字"));
        }
    }

    public boolean bootstrapTokenMatches(String expected, String supplied) {
        if (expected == null || expected.isBlank() || supplied == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
