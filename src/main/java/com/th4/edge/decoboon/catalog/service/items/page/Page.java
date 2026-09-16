package com.th4.edge.decoboon.catalog.service.items.page;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 家装材料库分页。 */
public interface Page extends Provider<CatalogItem, org.springframework.data.domain.Page<CatalogItem>>, Security {
}
