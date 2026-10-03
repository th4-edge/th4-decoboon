package com.th4.edge.components.wechat.configure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/** 微信小程序服务端配置。 */
@ConfigurationProperties(prefix = "th4.edge.components.wechat.mini-program")
public class WechatMiniProgramConfigure {

    private boolean enabled = true;
    private String appId = "";
    private String appSecret = "";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;

    /**
     * 订阅消息模板 ID，键为 {@link WechatSubscribeScenario} 中的场景键。
     *
     * <p>未配置（或配置为空串）的场景一律跳过发送，便于按场景逐步灰度上线。</p>
     */
    private Map<String, String> subscribeTemplates = new LinkedHashMap<>();

    public boolean isConfigured() {
        return enabled && appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }

    /** 取场景对应的模板 ID；未配置返回 {@code null}，调用方应跳过发送而不是报错。 */
    public String subscribeTemplate(String scenario) {
        if (scenario == null || subscribeTemplates == null) {
            return null;
        }
        String templateId = subscribeTemplates.get(scenario);
        return templateId == null || templateId.isBlank() ? null : templateId.trim();
    }

    public boolean isSubscribeConfigured(String scenario) {
        return subscribeTemplate(scenario) != null;
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
    public Map<String, String> getSubscribeTemplates() { return subscribeTemplates; }
    public void setSubscribeTemplates(Map<String, String> subscribeTemplates) {
        this.subscribeTemplates = subscribeTemplates == null ? new LinkedHashMap<>() : subscribeTemplates;
    }
}
