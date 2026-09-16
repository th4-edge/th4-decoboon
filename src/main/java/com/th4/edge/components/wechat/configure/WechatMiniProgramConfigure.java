package com.th4.edge.components.wechat.configure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 微信小程序服务端配置。 */
@ConfigurationProperties(prefix = "th4.edge.components.wechat.mini-program")
public class WechatMiniProgramConfigure {

    private boolean enabled = true;
    private String appId = "";
    private String appSecret = "";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;

    public boolean isConfigured() {
        return enabled && appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
