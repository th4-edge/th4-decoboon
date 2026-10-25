package com.th4.edge.decoboon.plan.service.constructiontemplate.build;

import com.th4.edge.decoboon.plan.model.CompanyConstructionPlan;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 创建施工计划下一版草稿或显式升级平台基线。 */
public interface Build extends Provider<CompanyConstructionPlan, ConstructionPlanTemplate>, Security {
}
