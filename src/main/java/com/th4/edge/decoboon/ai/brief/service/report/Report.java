package com.th4.edge.decoboon.ai.brief.service.report;

import com.th4.edge.decoboon.ai.brief.model.AiBriefQuery;
import com.th4.edge.decoboon.ai.brief.model.AiBriefReport;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** AI 经营参谋经营简报。 */
public interface Report extends Provider<AiBriefQuery, AiBriefReport>, Security {
}
