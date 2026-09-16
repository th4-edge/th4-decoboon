package com.th4.edge.platform.context;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** 平台独立密码策略和 BCrypt 实现。 */
@Note("平台密码上下文")
@Component
public class PlatformPasswordContext {
    private static final Pattern LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        validate(rawPassword);
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return rawPassword != null && hash != null && encoder.matches(rawPassword, hash);
    }

    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_PASSWORD_POLICY", "密码长度必须为 8-64 位"));
        }
        if (!LETTER.matcher(rawPassword).matches() || !DIGIT.matcher(rawPassword).matches()) {
            throw new DefinedException(new DefaultErrorCode("PLATFORM_PASSWORD_POLICY", "密码必须同时包含字母和数字"));
        }
    }
}
