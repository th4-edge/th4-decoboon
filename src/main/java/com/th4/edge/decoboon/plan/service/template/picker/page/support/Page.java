package com.th4.edge.decoboon.plan.service.template.picker.page.support;

import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.model.PaymentPlanNodeTemplate;
import com.th4.edge.decoboon.plan.model.ConstructionStageTemplate;
import com.th4.edge.decoboon.plan.model.PaymentPlanTemplate;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.PlanTemplateCatalog;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 付款/施工计划选择器分页实现。 */
@Note("付款施工计划选择器分页")
@Properties(allow = {
        @AllowProperties(model = PlanTemplateCatalog.class,
                properties = {"paymentPlanBaselineVersion", "constructionPlanBaselineVersion", "paymentPlans",
                        "constructionPlans", "total", "current", "size", "hasMore"}),
        @AllowProperties(model = PaymentPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "sort", "source",
                        "sourceBaselineCode", "sourceBaselineVersion", "status", "defaultPlan", "currentEnabled", "nodes"}),
        @AllowProperties(model = PaymentPlanNodeTemplate.class,
                properties = {"code", "name", "ratio", "type", "triggerType", "triggerStageCode", "offsetDays", "sort"}),
        @AllowProperties(model = ConstructionPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "totalDays", "sort", "source",
                        "sourceBaselineCode", "sourceBaselineVersion", "status", "defaultPlan", "currentEnabled", "stages"}),
        @AllowProperties(model = ConstructionStageTemplate.class,
                properties = {"code", "name", "defaultDays", "acceptanceMode", "sort", "description"})
})
@Service("plan/template/picker/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.plan.service.template.picker.page.Page {
    @User private Employee employee;
    @AutoInject private String kind;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private String keyword;
    private final CompanyPlanTemplateContext context;

    public Page(CompanyPlanTemplateContext context) {
        this.context = context;
    }

    @Override
    public PlanTemplateCatalog call(Employee request) {
        return context.pickerCatalog(employee, kind, current, size, keyword);
    }
}
