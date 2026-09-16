package com.th4.edge.components.wechat.service;

import java.util.Map;

/**
 * 微信支付通用能力；业务 Provider 只依赖本接口。
 */
public interface WechatPayClient {

    WechatJsapiPayParams prepay(WechatPayOrder order);

    WechatPayQuery query(String outTradeNo);

    void verifyNotification(String timestamp, String nonce, String body,
                            String signature, String serial);

    WechatPayNotification decryptNotification(Map<String, Object> resource);
}
