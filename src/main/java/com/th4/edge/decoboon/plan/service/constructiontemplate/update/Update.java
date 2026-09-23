package com.th4.edge.decoboon.plan.service.constructiontemplate.update;

import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** construction-template update Provider。 */
public interface Update extends Provider<CompanyConstructionPlan, ConstructionPlanTemplate>, Security {
}
