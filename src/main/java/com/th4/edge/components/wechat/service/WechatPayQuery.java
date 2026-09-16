package com.th4.edge.components.wechat.service;

/**
 * 微信支付订单查询结果。
 */
public record WechatPayQuery(
        String outTradeNo,
        String transactionId,
        String tradeState,
        long amount,
        String successTime,
        String payerOpenId
) {
    public boolean success() {
        return "SUCCESS".equals(tradeState);
    }
}
