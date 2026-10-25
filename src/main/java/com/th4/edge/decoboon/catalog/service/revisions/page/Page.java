package com.th4.edge.decoboon.catalog.service.revisions.page;

import com.th4.edge.decoboon.catalog.model.CatalogItemRevision;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 报价项目修订历史分页。 */
public interface Page extends Provider<CatalogItemRevision, org.springframework.data.domain.Page<CatalogItemRevision>>, Security {
}
