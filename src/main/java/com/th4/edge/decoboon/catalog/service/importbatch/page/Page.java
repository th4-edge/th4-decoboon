package com.th4.edge.decoboon.catalog.service.importbatch.page;

import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 报价项目库导入批次 page 能力。 */
public interface Page extends Provider<CatalogImportBatch, org.springframework.data.domain.Page<CatalogImportBatch>>, Security {
}
