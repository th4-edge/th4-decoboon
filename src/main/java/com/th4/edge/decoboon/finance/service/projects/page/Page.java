package com.th4.edge.decoboon.finance.service.projects.page;

import com.th4.edge.decoboon.finance.model.FinanceProjectReport;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目财务账本分页。 */
public interface Page extends Provider<Project, org.springframework.data.domain.Page<FinanceProjectReport>>, Security {
}
