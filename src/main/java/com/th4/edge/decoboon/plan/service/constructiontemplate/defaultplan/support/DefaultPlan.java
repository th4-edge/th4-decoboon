package com.th4.edge.decoboon.plan.service.constructiontemplate.defaultplan.support;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** plan/construction-template/default 实现。 */
@Note("plan/construction-template/default")
@Properties(allow = {
        @AllowProperties(model = ConstructionPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "totalDays", "sort", "source", "status", "defaultPlan", "stages"}),
        @AllowProperties(model = ConstructionStageTemplate.class,
                properties = {"code", "name", "ratio", "type", "triggerType", "triggerStageCode", "offsetDays", "sort", "defaultDays", "acceptanceMode", "description"})
})
@Service("plan/construction-template/default")
@Scope(ScopeTypes.PROTOTYPE)
public class DefaultPlan implements com.th4.edge.decoboon.plan.service.constructiontemplate.defaultplan.DefaultPlan {

    @User
    private Employee employee;

    @AutoInject
    private Long templateId;

    private final CompanyPlanTemplateContext context;

    public DefaultPlan(CompanyPlanTemplateContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = false)
    public ConstructionPlanTemplate call(CompanyConstructionPlan request) {
        CompanyConstructionPlan command = new CompanyConstructionPlan();
        command.setTemplateId(templateId);
        return context.defaultConstruction(employee, command);
    }
}
