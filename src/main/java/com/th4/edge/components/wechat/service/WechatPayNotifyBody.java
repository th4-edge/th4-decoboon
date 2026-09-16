package com.th4.edge.components.wechat.service;

/**
 * 当前微信支付回调的原始请求体。
 *
 * <p>仅在回调过滤器作用域内有效，用于按微信支付规范完成 RSA 验签。</p>
 */
public final class WechatPayNotifyBody {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private WechatPayNotifyBody() {
    }

    public static void set(String body) {
        CURRENT.set(body);
    }

    public static String current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
