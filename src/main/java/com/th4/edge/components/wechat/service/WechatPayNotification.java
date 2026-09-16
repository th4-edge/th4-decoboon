package com.th4.edge.components.wechat.service;

/**
 * 微信支付通知解密后的交易结果。
 */
public record WechatPayNotification(
        String appId,
        String mchId,
        String outTradeNo,
        String transactionId,
        String tradeState,
        long amount,
        String payerOpenId,
        String successTime
) {
    public boolean success() {
        return "SUCCESS".equals(tradeState);
    }
}
