package com.th4.edge.decoboon.plan.service.paymenttemplate.publish;

import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.model.CompanyPaymentPlan;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** payment-template publish Provider。 */
public interface Publish extends Provider<CompanyPaymentPlan, PaymentPlanTemplate>, Security {
}
