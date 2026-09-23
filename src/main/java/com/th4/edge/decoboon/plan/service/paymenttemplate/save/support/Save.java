package com.th4.edge.decoboon.plan.service.paymenttemplate.save.support;

import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.model.CompanyPaymentPlan;
import com.th4.edge.decoboon.plan.model.CompanyPaymentNode;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanNodeTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** plan/payment-template/save 实现。 */
@Note("plan/payment-template/save")
@Properties(allow = {
        @AllowProperties(model = PaymentPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "totalDays", "sort", "source", "status", "defaultPlan", "nodes"}),
        @AllowProperties(model = PaymentPlanNodeTemplate.class,
                properties = {"code", "name", "ratio", "type", "triggerType", "triggerStageCode", "offsetDays", "sort", "defaultDays", "acceptanceMode", "description"})
})
@Service("plan/payment-template/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.plan.service.paymenttemplate.save.Save {

    @User
    private Employee employee;

    @AutoInject
    private Long templateId;
    @AutoInject
    private String sourceBaselineCode;
    @AutoInject
    private String name;
    @AutoInject
    private String description;
    @AutoInject
    private Integer sort;
    @AutoInject
    private List<CompanyPaymentNode> nodes;

    private final CompanyPlanTemplateContext context;

    public Save(CompanyPlanTemplateContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = false)
    public PaymentPlanTemplate call(CompanyPaymentPlan request) {
        CompanyPaymentPlan command = new CompanyPaymentPlan();
        command.setTemplateId(templateId);
        command.setSourceBaselineCode(sourceBaselineCode);
        command.setName(name);
        command.setDescription(description);
        command.setSort(sort);
        command.setNodes(nodes);
        return context.savePayment(employee, command);
    }
}
