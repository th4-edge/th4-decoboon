package com.th4.edge.decoboon.common.file.service.grants.download;

import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.framework.provider.Provider;

/** 使用短期签名读取私有文件。 */
public interface Download extends Provider<FileGrant, java.io.File> {
}
