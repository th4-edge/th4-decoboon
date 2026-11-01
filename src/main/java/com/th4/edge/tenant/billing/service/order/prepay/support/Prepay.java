package com.th4.edge.tenant.billing.service.order.prepay.support;

import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.components.wechat.service.WechatJsapiPayParams;
import com.th4.edge.components.wechat.service.WechatPayClient;
import com.th4.edge.components.wechat.service.WechatPayOrder;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.billing.configure.OrderStatus;
import com.th4.edge.tenant.billing.configure.PayChannel;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Order;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 企业负责人发起微信小程序支付。
 */
@Note("微信支付预下单")
@Description("服务端校验企业订单归属、金额和登录微信身份后返回 requestPayment 参数")
@Properties(allow = {
        @AllowProperties(model = com.th4.edge.components.wechat.service.WechatJsapiPayParams.class,
                properties = {"appId", "timeStamp", "nonceStr", "package", "signType", "paySign"})
})
@Service("order/prepay")
@Scope(ScopeTypes.PROTOTYPE)
public class Prepay implements com.th4.edge.tenant.billing.service.order.prepay.Prepay {

    @User
    private Employee employee;

    @AutoInject
    @NotEmpty
    @Length(max = 40)
    private String orderNo;

    private final TenantContext tenantContext;
    private final SubscriptionContext subscriptionContext;
    private final UserIdentityContext userIdentityContext;
    private final WechatPayClient wechatPayClient;

    public Prepay(TenantContext tenantContext,
                  SubscriptionContext subscriptionContext,
                  UserIdentityContext userIdentityContext,
                  WechatPayClient wechatPayClient) {
        this.tenantContext = tenantContext;
        this.subscriptionContext = subscriptionContext;
        this.userIdentityContext = userIdentityContext;
        this.wechatPayClient = wechatPayClient;
    }

    @Override
    public WechatJsapiPayParams call(Order request) {
        Employee boss = tenantContext.requireBoss(employee);
        Order order = subscriptionContext.requireOrder(boss.getCompany(), orderNo);
        order = subscriptionContext.expireIfNecessary(order);
        if (order.getStatus() == OrderStatus.Granted) {
            throw new DefinedException(new DefaultErrorCode("400", "订单已支付并完成履约"));
        }
        if (order.getStatus() != OrderStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", "当前订单状态不能支付"));
        }
        if (order.getPayChannel() != PayChannel.Wx) {
            throw new DefinedException(new DefaultErrorCode("400", "当前订单不是微信支付订单"));
        }
        Credentials credentials = userIdentityContext.requireWx(boss);
        if (credentials == null || credentials.getOpenId() == null || credentials.getOpenId().isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "当前负责人尚未绑定微信登录身份"));
        }
        String planName = order.getPlanVersion() == null
                ? order.getPlan().getName()
                : order.getPlanVersion().getPlanName();
        return wechatPayClient.prepay(new WechatPayOrder(
                order.getOutTradeNo() == null ? order.getOrderNo() : order.getOutTradeNo(),
                order.getAmount(),
                "装得清-" + planName + "-" + order.getDays() + "天",
                credentials.getOpenId(),
                order.getExpireTime()));
    }
}
