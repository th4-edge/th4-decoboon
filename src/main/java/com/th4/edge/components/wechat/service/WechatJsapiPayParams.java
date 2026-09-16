package com.th4.edge.components.wechat.service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 微信小程序 requestPayment 所需参数。
 */
public record WechatJsapiPayParams(
        String appId,
        String timeStamp,
        String nonceStr,
        @JsonProperty("package") String packageValue,
        String signType,
        String paySign
) {
}
