package com.th4.edge.decoboon.catalog.service.importbatch.save;

import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 报价项目库导入批次 save 能力。 */
public interface Save extends Provider<CatalogImportBatch, CatalogImportBatch>, Security {
}
