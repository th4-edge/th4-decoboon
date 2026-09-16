package com.th4.edge.decoboon.common.file.service.grants.build;

import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 构建短期私有文件访问 URL。 */
public interface Build extends Provider<FileGrant, java.util.List<FileGrant>>, Security {
}
