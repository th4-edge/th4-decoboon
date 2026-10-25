package com.th4.edge.decoboon.plan.service.paymenttemplate.build;

import com.th4.edge.decoboon.plan.model.CompanyPaymentPlan;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 创建付款计划下一版草稿或显式升级平台基线。 */
public interface Build extends Provider<CompanyPaymentPlan, PaymentPlanTemplate>, Security {
}
