package com.th4.edge.decoboon.catalog.service.categories.tree;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.model.CatalogTree;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 家装材料库分类树。 */
public interface Tree extends Provider<CatalogItem, CatalogTree>, Security {
}
