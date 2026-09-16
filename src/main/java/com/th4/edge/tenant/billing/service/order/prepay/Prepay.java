package com.th4.edge.tenant.billing.service.order.prepay;

import com.th4.edge.components.wechat.service.WechatJsapiPayParams;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.billing.model.Order;

/**
 * 创建微信小程序支付参数。
 */
public interface Prepay extends Provider<Order, WechatJsapiPayParams>, Security {
}
