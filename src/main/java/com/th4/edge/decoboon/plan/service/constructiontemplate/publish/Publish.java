package com.th4.edge.decoboon.plan.service.constructiontemplate.publish;

import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** construction-template publish Provider。 */
public interface Publish extends Provider<CompanyConstructionPlan, ConstructionPlanTemplate>, Security {
}
