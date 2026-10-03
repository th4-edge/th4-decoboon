package com.th4.edge.components.wechat.service;

/**
 * 微信订阅消息发送结果。
 *
 * <p>用户未订阅（errcode 43101）是正常业务状态而不是系统故障：一次性订阅的额度
 * 用完、用户从未授权或已退订都会命中它。因此以结果对象返回而不抛异常，
 * 避免用户的一次未授权打断正在进行的业务事务。</p>
 */
public record WechatSubscribeResult(boolean delivered, int errorCode, String errorMessage) {

    /** 用户尚未授权或额度已用尽：属于正常状态，不应记为系统故障。 */
    public static final int NOT_SUBSCRIBED = 43101;

    public static WechatSubscribeResult success() {
        return new WechatSubscribeResult(true, 0, "");
    }

    public static WechatSubscribeResult failure(int errorCode, String errorMessage) {
        return new WechatSubscribeResult(false, errorCode, errorMessage == null ? "" : errorMessage);
    }

    public boolean notSubscribed() {
        return errorCode == NOT_SUBSCRIBED;
    }
}
