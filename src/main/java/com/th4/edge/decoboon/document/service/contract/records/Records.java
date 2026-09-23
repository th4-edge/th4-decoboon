package com.th4.edge.decoboon.document.service.contract.records;

import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 合同打印稿归档记录服务。 */
public interface Records extends Provider<ProjectContract, java.util.List<DocumentRecord>>, Security {
}
