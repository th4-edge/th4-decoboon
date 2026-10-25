package com.th4.edge.decoboon.plan.service.constructiontemplate.disable.support;

import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.decoboon.plan.model.CompanyConstructionStage;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.ConstructionStageTemplate;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

/** plan/construction-template/disable 实现。 */
@Note("plan/construction-template/disable")
@Properties(allow = {
        @AllowProperties(model = ConstructionPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "totalDays", "sort", "source", "sourceBaselineCode", "sourceBaselineVersion", "status", "defaultPlan", "currentEnabled", "stages"}),
        @AllowProperties(model = ConstructionStageTemplate.class,
                properties = {"code", "name", "ratio", "type", "triggerType", "triggerStageCode", "offsetDays", "sort", "defaultDays", "acceptanceMode", "description"})
})
@Service("plan/construction-template/disable")
@Scope(ScopeTypes.PROTOTYPE)
public class Disable implements com.th4.edge.decoboon.plan.service.constructiontemplate.disable.Disable {

    @User
    private Employee employee;

    @AutoInject
    private Long templateId;

    private final CompanyPlanTemplateContext context;

    public Disable(CompanyPlanTemplateContext context) {
        this.context = context;
    }

    @Override
    public ConstructionPlanTemplate call(CompanyConstructionPlan request) {
        CompanyConstructionPlan command = new CompanyConstructionPlan();
        command.setTemplateId(templateId);
        return context.disableConstruction(employee, command);
    }
}
