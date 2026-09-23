package com.th4.edge.decoboon.plan.service.paymenttemplate.defaultplan;

import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.model.CompanyPaymentPlan;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** payment-template defaultplan Provider。 */
public interface DefaultPlan extends Provider<CompanyPaymentPlan, PaymentPlanTemplate>, Security {
}
