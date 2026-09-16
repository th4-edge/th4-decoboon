package com.th4.edge.components.wechat.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.components.wechat.configure.WechatPayConfigure;
import com.th4.edge.components.wechat.service.WechatJsapiPayParams;
import com.th4.edge.components.wechat.service.WechatPayClient;
import com.th4.edge.components.wechat.service.WechatPayNotification;
import com.th4.edge.components.wechat.service.WechatPayOrder;
import com.th4.edge.components.wechat.service.WechatPayQuery;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付 API v3 JSAPI 客户端。
 */
@Component
@Configuration
@EnableConfigurationProperties(WechatPayConfigure.class)
public class WechatPaySdk implements WechatPayClient {

    private static final String API_BASE = "https://api.mch.weixin.qq.com";
    private static final DateTimeFormatter WECHAT_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final WechatPayConfigure configure;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WechatPaySdk(WechatPayConfigure configure, ObjectMapper objectMapper) {
        this.configure = configure;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configure.getConnectTimeoutMs()))
                .build();
    }

    @PostConstruct
    public void validateConfiguration() {
        if (!configure.isEnabled()) return;
        if (!configure.isConfigured()) {
            throw new IllegalStateException("微信支付配置不完整：请检查商户号、证书序列号、商户私钥、API v3 密钥、回调地址和微信支付公钥");
        }
        if (configure.getApiV3Key().getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("微信支付 API v3 密钥必须为 32 字节");
        }
        try {
            privateKey();
        } catch (Exception exception) {
            throw new IllegalStateException("微信支付商户私钥无法加载", exception);
        }
        try {
            platformPublicKey();
        } catch (Exception exception) {
            throw new IllegalStateException("微信支付公钥无法加载", exception);
        }
    }

    @Override
    public WechatJsapiPayParams prepay(WechatPayOrder order) {
        requireConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appid", configure.getAppId());
        body.put("mchid", configure.getMchId());
        body.put("description", truncate(order.description(), 127));
        body.put("out_trade_no", order.outTradeNo());
        body.put("time_expire", formatExpireTime(order.expireTime()));
        body.put("notify_url", configure.getNotifyUrl());
        body.put("amount", Map.of("total", order.amount(), "currency", "CNY"));
        body.put("payer", Map.of("openid", order.openId()));

        JsonNode result = request("POST", "/v3/pay/transactions/jsapi", body);
        String prepayId = result.path("prepay_id").asText("");
        if (prepayId.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("502", "微信支付未返回 prepay_id"));
        }
        return jsapiParams(prepayId);
    }

    @Override
    public WechatPayQuery query(String outTradeNo) {
        requireConfigured();
        if (outTradeNo == null || outTradeNo.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "商户订单号不能为空"));
        }
        String path = "/v3/pay/transactions/out-trade-no/" + encode(outTradeNo)
                + "?mchid=" + encode(configure.getMchId());
        JsonNode result = request("GET", path, null);
        return new WechatPayQuery(
                result.path("out_trade_no").asText(outTradeNo),
                result.path("transaction_id").asText(null),
                result.path("trade_state").asText(""),
                result.path("amount").path("total").asLong(0L),
                result.path("success_time").asText(null),
                result.path("payer").path("openid").asText(null));
    }

    @Override
    public void verifyNotification(String timestamp, String nonce, String body,
                                   String signature, String serial) {
        requireConfigured();
        if (timestamp == null || nonce == null || body == null || signature == null) {
            throw new DefinedException(new DefaultErrorCode("403", "微信支付回调签名缺失"));
        }
        if (serial != null && !serial.isBlank()
                && !configure.getPlatformPublicKeyId().equals(serial)) {
            throw new DefinedException(new DefaultErrorCode("403", "微信支付平台公钥标识不匹配"));
        }
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(platformPublicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signature))) {
                throw new DefinedException(new DefaultErrorCode("403", "微信支付回调验签失败"));
            }
        } catch (DefinedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DefinedException(new DefaultErrorCode("403", "微信支付回调验签失败"));
        }
    }

    @Override
    public WechatPayNotification decryptNotification(Map<String, Object> resource) {
        requireConfigured();
        if (resource == null) {
            throw new DefinedException(new DefaultErrorCode("400", "微信支付通知缺少 resource"));
        }
        String ciphertext = text(resource, "ciphertext");
        String nonce = text(resource, "nonce");
        String associatedData = text(resource, "associated_data");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] key = configure.getApiV3Key().getBytes(StandardCharsets.UTF_8);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
            if (!associatedData.isBlank()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            JsonNode result = objectMapper.readTree(plain);
            String appId = result.path("appid").asText("");
            String mchId = result.path("mchid").asText("");
            if (!configure.getAppId().equals(appId) || !configure.getMchId().equals(mchId)) {
                throw new DefinedException(new DefaultErrorCode("400", "微信支付通知商户信息不匹配"));
            }
            return new WechatPayNotification(
                    appId,
                    mchId,
                    result.path("out_trade_no").asText(""),
                    result.path("transaction_id").asText(null),
                    result.path("trade_state").asText(""),
                    result.path("amount").path("total").asLong(0L),
                    result.path("payer").path("openid").asText(null),
                    result.path("success_time").asText(null));
        } catch (DefinedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DefinedException(new DefaultErrorCode("400", "微信支付通知解密失败"));
        }
    }

    private WechatJsapiPayParams jsapiParams(String prepayId) {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String nonce = nonce();
        String packageValue = "prepay_id=" + prepayId;
        String message = configure.getAppId() + "\n" + timeStamp + "\n" + nonce + "\n"
                + packageValue + "\n";
        return new WechatJsapiPayParams(configure.getAppId(), timeStamp, nonce,
                packageValue, "RSA", sign(message));
    }

    private JsonNode request(String method, String path, Map<String, Object> body) {
        try {
            String payload = body == null ? "" : objectMapper.writeValueAsString(body);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
            String nonce = nonce();
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(API_BASE + path))
                    .timeout(Duration.ofMillis(configure.getReadTimeoutMs()))
                    .header("Accept", "application/json")
                    .header("User-Agent", "th4-decoboon/1.0")
                    .header("Authorization", authorization(method, path, timestamp, nonce, payload));
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            }
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode result = response.body() == null || response.body().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = result.path("message").asText(result.path("errmsg").asText("微信支付请求失败"));
                throw new DefinedException(new DefaultErrorCode(
                        String.valueOf(response.statusCode()), message));
            }
            return result;
        } catch (DefinedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DefinedException(new DefaultErrorCode("503", "微信支付服务暂时不可用"));
        }
    }

    private String authorization(String method, String path, String timestamp,
                                 String nonce, String payload) {
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n"
                + payload + "\n";
        return "WECHATPAY2-SHA256-RSA2048 mchid=\"" + configure.getMchId()
                + "\",nonce_str=\"" + nonce + "\",signature=\"" + sign(message)
                + "\",timestamp=\"" + timestamp + "\",serial_no=\""
                + configure.getMchSerialNo() + "\"";
    }

    private String sign(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception exception) {
            throw new DefinedException(new DefaultErrorCode("503", "微信支付商户私钥配置错误"));
        }
    }

    private PublicKey platformPublicKey() throws Exception {
        String pem = configure.getPlatformPublicKey();
        if (pem == null || pem.isBlank()) {
            pem = Files.readString(Path.of(configure.getPlatformPublicKeyPath()), StandardCharsets.UTF_8);
        }
        if (pem.contains("BEGIN CERTIFICATE")) {
            return CertificateFactory.getInstance("X.509").generateCertificate(
                    new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))).getPublicKey();
        }
        String normalized = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(normalized)));
    }

    private PrivateKey privateKey() throws Exception {
        String pem = configure.getPrivateKey();
        if (pem == null || pem.isBlank()) {
            pem = Files.readString(Path.of(configure.getPrivateKeyPath()), StandardCharsets.UTF_8);
        }
        String normalized = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private String text(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "微信支付通知缺少 " + key));
        }
        return value.toString();
    }

    private String formatExpireTime(LocalDateTime expireTime) {
        LocalDateTime value = expireTime == null ? LocalDateTime.now().plusMinutes(30) : expireTime;
        return OffsetDateTime.of(value, ZoneOffset.ofHours(8)).format(WECHAT_TIME);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String truncate(String value, int maxLength) {
        String text = value == null ? "" : value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String nonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void requireConfigured() {
        if (!configure.isConfigured()) {
            throw new DefinedException(new DefaultErrorCode(
                    "503", "微信支付商户号、API v3 密钥、商户私钥或回调地址尚未配置"));
        }
    }
}
