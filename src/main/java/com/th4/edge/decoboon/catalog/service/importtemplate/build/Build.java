package com.th4.edge.decoboon.catalog.service.importtemplate.build;

import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 报价项目库批量导入模板下载（只读公共资源，返回私有文件下载授权）。 */
public interface Build extends Provider<FileGrant, FileGrant>, Security {
}
