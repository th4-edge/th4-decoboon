package com.th4.edge.decoboon.customer.service.sharelink.save;

import com.th4.edge.decoboon.customer.model.ProjectShareLink;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主生成亲友观看链接。 */
public interface Save extends Provider<ProjectShareLink, ProjectShareLink>, Security {
}
