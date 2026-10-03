package com.th4.edge.components.wechat.service;

/** 微信小程序身份、手机号授权与订阅消息能力。 */
public interface WechatMiniProgramClient {

    WechatSession session(String code);

    WechatPhone phone(String phoneCode);

    /**
     * 发送订阅消息。
     *
     * <p>发送失败（含用户未订阅 43101、模板未配置、微信服务不可用）以结果对象返回，
     * 不抛异常：通知失败不得回滚业务事实。</p>
     */
    WechatSubscribeResult subscribeMessage(WechatSubscribeMessage message);
}
