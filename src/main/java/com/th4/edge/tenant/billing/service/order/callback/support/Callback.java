package com.th4.edge.tenant.billing.service.order.callback.support;


import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.components.wechat.service.WechatPayClient;
import com.th4.edge.components.wechat.service.WechatPayNotification;
import com.th4.edge.components.wechat.service.WechatPayNotifyBody;
import com.th4.edge.components.wechat.service.WechatPayQuery;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoHeader;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.tenant.billing.configure.PayChannel;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Order;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 微信支付通知入口，解密并二次查询后幂等履约。
 */
@Note("微信支付回调")
@Description("仅接受微信支付 API v3 加密通知；交易状态和金额以微信支付查询结果为准")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.tenant.billing.model.Order.class,
                properties = {"orderNo", "status", "amount", "paidTime"})
})
@Service("order/callback")
@Scope(ScopeTypes.PROTOTYPE)
public class Callback implements com.th4.edge.tenant.billing.service.order.callback.Callback {

    @AutoHeader("Wechatpay-Timestamp")
    private String timestamp;

    @AutoHeader("Wechatpay-Nonce")
    private String nonce;

    @AutoHeader("Wechatpay-Signature")
    private String signature;

    @AutoHeader("Wechatpay-Serial")
    private String serial;

    @AutoInject("event_type")
    private String eventType;

    @AutoInject
    private Map<String, Object> resource;

    private final WechatPayClient wechatPayClient;
    private final SubscriptionContext subscriptionContext;

    public Callback(WechatPayClient wechatPayClient, SubscriptionContext subscriptionContext) {
        this.wechatPayClient = wechatPayClient;
        this.subscriptionContext = subscriptionContext;
    }

    @Override
    public Order call(Order request) {
        String body = WechatPayNotifyBody.current();
        if (body == null || body.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("403", "微信支付回调原始报文不存在"));
        }
        wechatPayClient.verifyNotification(timestamp, nonce, body, signature, serial);
        if (eventType != null && !eventType.isBlank() && !"TRANSACTION.SUCCESS".equals(eventType)) {
            throw new DefinedException(new DefaultErrorCode("400", "暂不支持的微信支付通知类型"));
        }
        WechatPayNotification notification = wechatPayClient.decryptNotification(resource);
        if (!notification.success()) {
            throw new DefinedException(new DefaultErrorCode("400", "微信支付交易尚未成功"));
        }
        WechatPayQuery payment = wechatPayClient.query(notification.outTradeNo());
        if (!payment.success()) {
            throw new DefinedException(new DefaultErrorCode("400", "微信支付交易状态未确认成功"));
        }
        if (payment.amount() != notification.amount()
                || notification.transactionId() != null
                && !notification.transactionId().equals(payment.transactionId())) {
            throw new DefinedException(new DefaultErrorCode("400", "微信支付通知与查询结果不一致"));
        }
        return subscriptionContext.fulfillOrder(notification.outTradeNo(),
                payment.outTradeNo(), payment.transactionId(), payment.amount(), PayChannel.Wx);
    }
}
