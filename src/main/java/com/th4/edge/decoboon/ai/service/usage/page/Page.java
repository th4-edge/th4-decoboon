package com.th4.edge.decoboon.ai.service.usage.page;

import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.ai.model.AiUsageQuery;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
/** AI 调用明细分页。 */
public interface Page extends Provider<AiUsageQuery, org.springframework.data.domain.Page<AiUsageCall>>, Security {
}
