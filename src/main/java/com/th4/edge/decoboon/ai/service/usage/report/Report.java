package com.th4.edge.decoboon.ai.service.usage.report;

import com.th4.edge.decoboon.ai.model.AiUsageQuery;
import com.th4.edge.decoboon.ai.model.AiUsageReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 企业 AI 资源用量报表。 */
public interface Report extends Provider<AiUsageQuery, AiUsageReport>, Security {
}
