package com.th4.edge.components.wechat.service;

/** 微信小程序身份、手机号授权与订阅消息能力。 */
public interface WechatMiniProgramClient {

    WechatSession session(String code);

    WechatPhone phone(String phoneCode);

    /**
     * 生成小程序码（`wxa/getwxacodeunlimit`，不限量接口）。
     *
     * <p>入参约定：{@code scene} 为进入页参数（≤32 字符，只放项目短码，禁止放手机号等敏感信息）；
     * {@code page} 为目标页面（须为已存在的页面路径）；{@code envVersion} 为
     * {@code release} / {@code trial} / {@code develop}，决定扫码打开的是正式版还是体验版。</p>
     *
     * <p>返回 JPEG 图片字节；微信返回错误码时抛出业务异常（不落半成品）。</p>
     */
    byte[] unlimitedQrCode(String scene, String page, String envVersion);

    /**
     * 发送订阅消息。
     *
     * <p>发送失败（含用户未订阅 43101、模板未配置、微信服务不可用）以结果对象返回，
     * 不抛异常：通知失败不得回滚业务事实。</p>
     */
    WechatSubscribeResult subscribeMessage(WechatSubscribeMessage message);
}
