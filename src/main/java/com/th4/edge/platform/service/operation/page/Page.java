package com.th4.edge.platform.service.operation.page;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.platform.model.PlatformOperationLog;
import com.th4.edge.platform.model.PlatformUser;

/** 平台操作审计分页。 */
public interface Page extends Provider<PlatformUser, org.springframework.data.domain.Page<PlatformOperationLog>>, Security {
}
