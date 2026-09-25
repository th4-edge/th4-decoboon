package com.th4.edge.decoboon.catalog.service.importrow.page;

import com.th4.edge.decoboon.catalog.model.CatalogImportRow;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 报价项目库导入行分页（预览、重复项与错误清单）。 */
public interface Page extends Provider<CatalogImportRow, org.springframework.data.domain.Page<CatalogImportRow>>, Security {
}
