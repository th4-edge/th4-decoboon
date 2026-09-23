package com.th4.edge.decoboon.plan.service.constructiontemplate.defaultplan;

import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** construction-template defaultplan Provider。 */
public interface DefaultPlan extends Provider<CompanyConstructionPlan, ConstructionPlanTemplate>, Security {
}
