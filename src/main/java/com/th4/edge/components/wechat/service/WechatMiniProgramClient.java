package com.th4.edge.components.wechat.service;

/** 微信小程序身份与手机号授权能力。 */
public interface WechatMiniProgramClient {

    WechatSession session(String code);

    WechatPhone phone(String phoneCode);
}
