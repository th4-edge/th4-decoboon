package com.th4.edge.components.wechat.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.components.wechat.configure.WechatMiniProgramConfigure;
import com.th4.edge.components.wechat.service.WechatMiniProgramClient;
import com.th4.edge.components.wechat.service.WechatPhone;
import com.th4.edge.components.wechat.service.WechatSession;
import com.th4.edge.components.wechat.service.WechatSubscribeMessage;
import com.th4.edge.components.wechat.service.WechatSubscribeResult;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/** 微信小程序 HTTP 客户端，AccessToken 在进程内安全缓存。 */
@Component
@Configuration
@EnableConfigurationProperties(WechatMiniProgramConfigure.class)
public class WechatMiniProgramSdk implements WechatMiniProgramClient {

    private static final Logger log = LoggerFactory.getLogger(WechatMiniProgramSdk.class);

    private final WechatMiniProgramConfigure configure;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private volatile String accessToken;
    private volatile Instant accessTokenExpireAt = Instant.EPOCH;

    public WechatMiniProgramSdk(WechatMiniProgramConfigure configure, ObjectMapper objectMapper) {
        this.configure = configure;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configure.getConnectTimeoutMs()))
                .build();
    }

    @Override
    public WechatSession session(String code) {
        requireConfigured();
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid="
                + encode(configure.getAppId()) + "&secret=" + encode(configure.getAppSecret())
                + "&js_code=" + encode(code) + "&grant_type=authorization_code";
        JsonNode body = get(url);
        requireSuccess(body, "微信登录失败");
        String openId = body.path("openid").asText("");
        if (openId.isBlank()) throw new DefinedException(new DefaultErrorCode("400", "微信登录未返回 openId"));
        return new WechatSession(openId, body.path("unionid").asText(null));
    }

    @Override
    public WechatPhone phone(String phoneCode) {
        requireConfigured();
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken();
        JsonNode body = post(url, "{\"code\":\"" + phoneCode.replace("\"", "") + "\"}");
        requireSuccess(body, "微信手机号授权失败");
        String phone = body.path("phone_info").path("purePhoneNumber").asText("");
        if (phone.isBlank()) throw new DefinedException(new DefaultErrorCode("400", "未获得微信授权手机号"));
        return new WechatPhone(phone);
    }

    @Override
    public byte[] unlimitedQrCode(String scene, String page, String envVersion) {
        requireConfigured();
        if (scene == null || scene.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "小程序码 scene 不能为空"));
        }
        String normalizedScene = scene.trim();
        if (normalizedScene.length() > 32) {
            // 微信硬限制：scene 最多 32 个可见字符，超长会返回 41030/invalid length。
            throw new DefinedException(new DefaultErrorCode("400", "小程序码 scene 最长 32 个字符"));
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("scene", normalizedScene);
        if (page != null && !page.isBlank()) {
            payload.put("page", page.trim());
        }
        // check_path=false：页面在已发布版本里尚未存在时仍可生成（发布前预览与体验版验证必需）。
        payload.put("check_path", false);
        payload.put("width", 430);
        payload.put("is_hyaline", false);
        if (envVersion != null && !envVersion.isBlank()) {
            payload.put("env_version", envVersion.trim());
        }
        byte[] body = postBinary("https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken(),
                payload.toString());
        if (body == null || body.length == 0) {
            throw new DefinedException(new DefaultErrorCode("502", "微信未返回小程序码"));
        }
        if (body[0] == '{') {
            // 失败时微信返回 JSON（如 40164 IP 未白名单 / 41030 参数非法），必须显式失败而不是把 JSON 当图片存下来。
            try {
                JsonNode error = objectMapper.readTree(body);
                int code = error.path("errcode").asInt(-1);
                String message = error.path("errmsg").asText("未知错误");
                log.warn("微信小程序码生成失败: errcode={} errmsg={}", code, message);
                throw new DefinedException(new DefaultErrorCode("502", "微信小程序码生成失败：" + message));
            } catch (DefinedException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new DefinedException(new DefaultErrorCode("502", "微信小程序码生成失败：响应无法解析"));
            }
        }
        return body;
    }

    private byte[] postBinary(String url, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(configure.getReadTimeoutMs()))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.body();
        } catch (Exception exception) {
            throw new DefinedException(new DefaultErrorCode("502", "微信小程序码请求异常：" + exception.getMessage()));
        }
    }

    @Override
    public WechatSubscribeResult subscribeMessage(WechatSubscribeMessage message) {
        if (message == null || message.openId() == null || message.openId().isBlank()) {
            return WechatSubscribeResult.failure(0, "缺少微信用户标识");
        }
        if (message.templateId() == null || message.templateId().isBlank()) {
            return WechatSubscribeResult.failure(0, "缺少订阅消息模板 ID");
        }
        if (!configure.isConfigured()) {
            return WechatSubscribeResult.failure(0, "微信小程序 appId/appSecret 尚未配置");
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("touser", message.openId());
            payload.put("template_id", message.templateId());
            if (message.page() != null && !message.page().isBlank()) {
                payload.put("page", message.page());
            }
            ObjectNode data = payload.putObject("data");
            if (message.data() != null) {
                message.data().forEach((key, value) -> {
                    if (key == null || key.isBlank()) {
                        return;
                    }
                    // 微信要求字段值非空，空值会被判定为参数不合法（47003）。
                    data.putObject(key).put("value", value == null || value.isBlank() ? "—" : value);
                });
            }
            JsonNode body = post("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token="
                    + accessToken(), objectMapper.writeValueAsString(payload));
            int errorCode = body.path("errcode").asInt(0);
            if (errorCode == 0) {
                return WechatSubscribeResult.success();
            }
            return WechatSubscribeResult.failure(errorCode, body.path("errmsg").asText(""));
        } catch (Exception exception) {
            return WechatSubscribeResult.failure(0, "微信订阅消息发送异常：" + exception.getMessage());
        }
    }

    private String accessToken() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpireAt)) return accessToken;
        synchronized (this) {
            if (accessToken != null && Instant.now().isBefore(accessTokenExpireAt)) return accessToken;
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + encode(configure.getAppId()) + "&secret=" + encode(configure.getAppSecret());
            JsonNode body = get(url);
            requireSuccess(body, "获取微信 AccessToken 失败");
            accessToken = body.path("access_token").asText("");
            accessTokenExpireAt = Instant.now().plusSeconds(Math.max(60, body.path("expires_in").asLong(7200) - 300));
            return accessToken;
        }
    }

    private JsonNode get(String url) {
        try {
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofMillis(configure.getReadTimeoutMs())).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readTree(response.body());
        } catch (Exception exception) {
            throw new DefinedException(exception, new DefaultErrorCode("503", "微信服务暂时不可用"));
        }
    }

    private JsonNode post(String url, String body) {
        try {
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofMillis(configure.getReadTimeoutMs()))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return objectMapper.readTree(response.body());
        } catch (Exception exception) {
            throw new DefinedException(exception, new DefaultErrorCode("503", "微信服务暂时不可用"));
        }
    }

    private void requireSuccess(JsonNode body, String message) {
        int errorCode = body.path("errcode").asInt(0);
        if (errorCode != 0) {
            throw new DefinedException(new DefaultErrorCode("400", message + "：" + body.path("errmsg").asText()));
        }
    }

    private void requireConfigured() {
        if (!configure.isConfigured()) {
            throw new DefinedException(new DefaultErrorCode("503", "微信小程序 appId/appSecret 尚未配置"));
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
