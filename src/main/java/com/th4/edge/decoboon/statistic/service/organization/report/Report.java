package com.th4.edge.decoboon.statistic.service.organization.report;

import com.th4.edge.decoboon.statistic.model.OrganizationQuery;
import com.th4.edge.decoboon.statistic.model.OrganizationReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业组织效能报表。 */
public interface Report extends Provider<OrganizationQuery, OrganizationReport>, Security {
}
