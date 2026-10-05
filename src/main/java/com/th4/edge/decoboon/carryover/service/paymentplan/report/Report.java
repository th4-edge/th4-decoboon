package com.th4.edge.decoboon.carryover.service.paymentplan.report;

import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlan;
import com.th4.edge.decoboon.carryover.model.CarryoverPaymentPlanReport;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 查看剩余收付款计划与未排期金额。 */
public interface Report extends ModelService<CarryoverPaymentPlan, CarryoverPaymentPlanReport>, Security {
}
