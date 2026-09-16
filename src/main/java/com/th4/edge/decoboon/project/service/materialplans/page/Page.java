package com.th4.edge.decoboon.project.service.materialplans.page;

import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 材料计划分页服务。 */
public interface Page extends Provider<MaterialPlan, org.springframework.data.domain.Page<MaterialPlan>>, Security {
}
