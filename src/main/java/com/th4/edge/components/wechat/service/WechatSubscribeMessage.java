package com.th4.edge.components.wechat.service;

import java.util.Map;

/**
 * 微信小程序订阅消息发送请求。
 *
 * <p>{@code data} 的 key 必须与公众平台所选模板的字段名完全一致（如 thing1、time2），
 * 由模板决定、不能自定义；字段值有类型和长度限制，超长会被微信拒绝（errcode 47003）。</p>
 */
public record WechatSubscribeMessage(String openId, String templateId, String page, Map<String, String> data) {}
