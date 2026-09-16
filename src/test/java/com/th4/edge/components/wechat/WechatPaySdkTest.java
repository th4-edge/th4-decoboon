package com.th4.edge.components.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.components.wechat.configure.WechatPayConfigure;
import com.th4.edge.components.wechat.sdk.WechatPayNotifyBodyFilter;
import com.th4.edge.components.wechat.sdk.WechatPaySdk;
import com.th4.edge.components.wechat.service.WechatPayNotification;
import com.th4.edge.components.wechat.service.WechatPayNotifyBody;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WechatPaySdkTest {

    private static final String API_V3_KEY = "0123456789abcdef0123456789abcdef";

    @Test
    void shouldCacheRawCallbackBodyOnlyDuringRequest() throws Exception {
        String body = "{\"id\":\"notify-body\"}";
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/order/callback");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> bodyInScope = new AtomicReference<>();
        AtomicReference<byte[]> bodyForDecoder = new AtomicReference<>();

        new WechatPayNotifyBodyFilter().doFilter(request, response, (downstreamRequest, ignored) -> {
            bodyInScope.set(WechatPayNotifyBody.current());
            bodyForDecoder.set(downstreamRequest.getInputStream().readAllBytes());
        });

        assertThat(bodyInScope.get()).isEqualTo(body);
        assertThat(new String(bodyForDecoder.get(), StandardCharsets.UTF_8)).isEqualTo(body);
        assertThat(WechatPayNotifyBody.current()).isNull();
    }

    @Test
    void shouldVerifyWechatPayNotificationSignature() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String publicKey = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
        WechatPayConfigure configure = configure(publicKey);
        WechatPaySdk sdk = new WechatPaySdk(configure, new ObjectMapper());

        String timestamp = "1789466400";
        String nonce = "nonce-for-signature";
        String body = "{\"id\":\"notify-1\",\"event_type\":\"TRANSACTION.SUCCESS\"}";
        String message = timestamp + "\n" + nonce + "\n" + body + "\n";
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());

        sdk.verifyNotification(timestamp, nonce, body, signature, "PUB_KEY_ID_TEST");
        assertThatThrownBy(() -> sdk.verifyNotification(timestamp, nonce,
                body + " ", signature, "PUB_KEY_ID_TEST"))
                .hasMessageContaining("验签失败");
    }

    @Test
    void shouldDecryptAndValidateWechatPayNotification() throws Exception {
        String plaintext = """
                {"appid":"wx-test","mchid":"mch-test","out_trade_no":"JZB202609150001",
                 "transaction_id":"4200000001","trade_state":"SUCCESS","success_time":"2026-09-15T10:00:00+08:00",
                 "payer":{"openid":"openid-test"},"amount":{"total":1198000}}
                """;
        WechatPaySdk sdk = sdk();
        WechatPayNotification notification = sdk.decryptNotification(encryptedResource(
                plaintext, API_V3_KEY, "nonce-123456", "transaction"));

        assertThat(notification.success()).isTrue();
        assertThat(notification.outTradeNo()).isEqualTo("JZB202609150001");
        assertThat(notification.transactionId()).isEqualTo("4200000001");
        assertThat(notification.amount()).isEqualTo(1198000L);
        assertThat(notification.payerOpenId()).isEqualTo("openid-test");
    }

    @Test
    void shouldRejectNotificationForAnotherMerchant() throws Exception {
        String plaintext = """
                {"appid":"wx-other","mchid":"mch-other","out_trade_no":"JZB202609150002",
                 "transaction_id":"4200000002","trade_state":"SUCCESS",
                 "payer":{"openid":"openid-test"},"amount":{"total":100}}
                """;
        WechatPaySdk sdk = sdk();
        assertThatThrownBy(() -> sdk.decryptNotification(encryptedResource(
                plaintext, API_V3_KEY, "nonce-abcdefgh", "transaction")))
                .hasMessageContaining("商户信息不匹配");
    }

    private WechatPaySdk sdk() {
        return new WechatPaySdk(configure("configured-public-key"), new ObjectMapper());
    }

    private WechatPayConfigure configure(String platformPublicKey) {
        WechatPayConfigure configure = new WechatPayConfigure();
        configure.setEnabled(true);
        configure.setAppId("wx-test");
        configure.setMchId("mch-test");
        configure.setMchSerialNo("serial-test");
        configure.setPrivateKey("configured-for-decryption-test");
        configure.setApiV3Key(API_V3_KEY);
        configure.setNotifyUrl("https://example.com/api/order/callback");
        configure.setPlatformPublicKeyId("PUB_KEY_ID_TEST");
        configure.setPlatformPublicKey(platformPublicKey);
        return configure;
    }

    private Map<String, Object> encryptedResource(String plaintext, String key,
                                                   String nonce, String associatedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        String ciphertext = Base64.getEncoder().encodeToString(
                cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        return Map.of(
                "algorithm", "AEAD_AES_256_GCM",
                "ciphertext", ciphertext,
                "nonce", nonce,
                "associated_data", associatedData);
    }
}
