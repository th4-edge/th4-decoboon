package com.th4.edge.decoboon.plan.service.template.page.support;

import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.ConstructionStageTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanNodeTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.model.PlanTemplateCatalog;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Note("平台付款与施工计划模板")
@Properties(allow = {
        @AllowProperties(model = PlanTemplateCatalog.class,
                properties = {"paymentPlanBaselineVersion", "constructionPlanBaselineVersion", "paymentPlans", "constructionPlans"}),
        @AllowProperties(model = PaymentPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "sort", "source", "status", "defaultPlan", "nodes"}),
        @AllowProperties(model = PaymentPlanNodeTemplate.class,
                properties = {"code", "name", "ratio", "type", "triggerType", "triggerStageCode", "offsetDays", "sort"}),
        @AllowProperties(model = ConstructionPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "totalDays", "sort", "source", "status", "defaultPlan", "stages"}),
        @AllowProperties(model = ConstructionStageTemplate.class,
                properties = {"code", "name", "defaultDays", "acceptanceMode", "sort", "description"})
})
@Service("plan/template/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.plan.service.template.page.Page {
    @User private Employee employee;
    private final CompanyPlanTemplateContext templateContext;

    public Page(CompanyPlanTemplateContext templateContext) {
        this.templateContext = templateContext;
    }

    @Override
    @Transactional(readOnly = true)
    public PlanTemplateCatalog call(Employee request) {
        return templateContext.availableCatalog(employee);
    }
}
