package com.th4.edge.decoboon.plan.service.constructiontemplate.update.support;

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

/** plan/construction-template/update 实现。 */
@Note("plan/construction-template/update")
@Properties(allow = {
        @AllowProperties(model = ConstructionPlanTemplate.class,
                properties = {"templateId", "code", "version", "name", "description", "totalDays", "sort", "source", "sourceBaselineCode", "sourceBaselineVersion", "status", "defaultPlan", "currentEnabled", "stages"}),
        @AllowProperties(model = ConstructionStageTemplate.class,
                properties = {"code", "name", "ratio", "type", "triggerType", "triggerStageCode", "offsetDays", "sort", "defaultDays", "acceptanceMode", "description"})
})
@Service("plan/construction-template/update")
@Scope(ScopeTypes.PROTOTYPE)
public class Update implements com.th4.edge.decoboon.plan.service.constructiontemplate.update.Update {

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
    private Integer totalDays;
    @AutoInject
    private List<CompanyConstructionStage> stages;

    private final CompanyPlanTemplateContext context;

    public Update(CompanyPlanTemplateContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = false)
    public ConstructionPlanTemplate call(CompanyConstructionPlan request) {
        CompanyConstructionPlan command = new CompanyConstructionPlan();
        command.setTemplateId(templateId);
        command.setSourceBaselineCode(sourceBaselineCode);
        command.setName(name);
        command.setDescription(description);
        command.setSort(sort);
        command.setTotalDays(totalDays);
        command.setStages(stages);
        return context.updateConstruction(employee, command);
    }
}
