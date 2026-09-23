package com.th4.edge.decoboon.plan.service.constructiontemplate.save;

import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** construction-template save Provider。 */
public interface Save extends Provider<CompanyConstructionPlan, ConstructionPlanTemplate>, Security {
}
