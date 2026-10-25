package com.th4.edge.decoboon.plan.service.paymenttemplate.build.support;

import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.model.CompanyPaymentPlan;
import com.th4.edge.decoboon.plan.model.PaymentPlanNodeTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** plan/payment-template/build 实现。 */
@Note("创建付款计划下一版")
@Properties(allow = {
        @AllowProperties(model = PaymentPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "sort", "source", "sourceBaselineCode", "sourceBaselineVersion", "status",
                        "defaultPlan", "currentEnabled", "nodes"}),
        @AllowProperties(model = PaymentPlanNodeTemplate.class,
                properties = {"code", "name", "ratio", "type", "triggerType", "triggerStageCode", "offsetDays", "sort"})
})
@Service("plan/payment-template/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.plan.service.paymenttemplate.build.Build {

    @AutoInject
    @NotNull
    private Long templateId;
    @AutoInject
    private String sourceBaselineCode;
    @User
    private Employee employee;

    private final CompanyPlanTemplateContext context;

    public Build(CompanyPlanTemplateContext context) {
        this.context = context;
    }

    @Override
    public PaymentPlanTemplate call(CompanyPaymentPlan request) {
        CompanyPaymentPlan command = new CompanyPaymentPlan();
        command.setTemplateId(templateId);
        command.setSourceBaselineCode(sourceBaselineCode);
        return context.buildPayment(employee, command);
    }
}
