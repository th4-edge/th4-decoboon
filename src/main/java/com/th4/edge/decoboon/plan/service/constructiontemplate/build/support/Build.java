package com.th4.edge.decoboon.plan.service.constructiontemplate.build.support;

import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.ConstructionStageTemplate;
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

/** plan/construction-template/build 实现。 */
@Note("创建施工计划下一版")
@Properties(allow = {
        @AllowProperties(model = ConstructionPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "totalDays", "sort", "source",
                        "status", "defaultPlan", "currentEnabled", "stages"}),
        @AllowProperties(model = ConstructionStageTemplate.class,
                properties = {"code", "name", "defaultDays", "acceptanceMode", "sort", "description"})
})
@Service("plan/construction-template/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.plan.service.constructiontemplate.build.Build {

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
    public ConstructionPlanTemplate call(CompanyConstructionPlan request) {
        CompanyConstructionPlan command = new CompanyConstructionPlan();
        command.setTemplateId(templateId);
        command.setSourceBaselineCode(sourceBaselineCode);
        return context.buildConstruction(employee, command);
    }
}
