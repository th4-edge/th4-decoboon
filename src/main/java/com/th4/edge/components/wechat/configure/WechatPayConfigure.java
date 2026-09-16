package com.th4.edge.components.wechat.configure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信支付 API v3 商户配置。
 */
@ConfigurationProperties(prefix = "th4.edge.components.wechat.pay")
public class WechatPayConfigure {

    private boolean enabled = true;
    private String appId = "";
    private String mchId = "";
    private String mchSerialNo = "";
    private String privateKey = "";
    private String privateKeyPath = "";
    private String apiV3Key = "";
    private String notifyUrl = "";
    private String platformPublicKeyId = "";
    private String platformPublicKey = "";
    private String platformPublicKeyPath = "";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 8000;

    public boolean isConfigured() {
        return enabled
                && appId != null && !appId.isBlank()
                && mchId != null && !mchId.isBlank()
                && mchSerialNo != null && !mchSerialNo.isBlank()
                && (hasText(privateKey) || hasText(privateKeyPath))
                && hasText(apiV3Key)
                && notifyUrl != null && !notifyUrl.isBlank()
                && hasText(platformPublicKeyId)
                && (hasText(platformPublicKey) || hasText(platformPublicKeyPath));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getMchId() { return mchId; }
    public void setMchId(String mchId) { this.mchId = mchId; }
    public String getMchSerialNo() { return mchSerialNo; }
    public void setMchSerialNo(String mchSerialNo) { this.mchSerialNo = mchSerialNo; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
    public String getApiV3Key() { return apiV3Key; }
    public void setApiV3Key(String apiV3Key) { this.apiV3Key = apiV3Key; }
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    public String getPlatformPublicKeyId() { return platformPublicKeyId; }
    public void setPlatformPublicKeyId(String platformPublicKeyId) { this.platformPublicKeyId = platformPublicKeyId; }
    public String getPlatformPublicKey() { return platformPublicKey; }
    public void setPlatformPublicKey(String platformPublicKey) { this.platformPublicKey = platformPublicKey; }
    public String getPlatformPublicKeyPath() { return platformPublicKeyPath; }
    public void setPlatformPublicKeyPath(String platformPublicKeyPath) { this.platformPublicKeyPath = platformPublicKeyPath; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
