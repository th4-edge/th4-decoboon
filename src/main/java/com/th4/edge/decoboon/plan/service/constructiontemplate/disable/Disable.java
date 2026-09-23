package com.th4.edge.decoboon.plan.service.constructiontemplate.disable;

import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** construction-template disable Provider。 */
public interface Disable extends Provider<CompanyConstructionPlan, ConstructionPlanTemplate>, Security {
}
