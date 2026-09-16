package com.th4.edge.components.wechat.service;

import java.time.LocalDateTime;

/**
 * 微信支付下单指令。
 */
public record WechatPayOrder(
        String outTradeNo,
        long amount,
        String description,
        String openId,
        LocalDateTime expireTime
) {
}
