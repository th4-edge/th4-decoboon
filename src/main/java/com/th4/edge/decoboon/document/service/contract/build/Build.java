package com.th4.edge.decoboon.document.service.contract.build;

import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 合同打印稿生成归档服务。 */
public interface Build extends Provider<ProjectContract, DocumentRecord>, Security {
}
