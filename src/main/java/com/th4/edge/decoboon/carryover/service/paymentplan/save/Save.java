package com.th4.edge.decoboon.carryover.service.paymentplan.save;

import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlan;
import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlanReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 企业负责人/财务整批保存剩余收付款计划。 */
public interface Save extends ModelService<CarryoverPaymentPlan, CarryoverPaymentPlanReport>, Security {
}
