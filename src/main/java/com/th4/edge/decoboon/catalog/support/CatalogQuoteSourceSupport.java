package com.th4.edge.decoboon.catalog.support;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.quote.model.QuoteLine;

/** 报价行来源修订映射。 */
public final class CatalogQuoteSourceSupport {

    private CatalogQuoteSourceSupport() {}

    public static void attach(QuoteLine line, CatalogItem item) {
        line.setCatalogItem(item);
        if (item != null) {
            line.setCatalogSourceId(item.getItemId());
            line.setCatalogSourceCode(item.getCode());
            line.setCatalogSourceRevision(item.getRevision() == null ? 1 : item.getRevision());
        }
    }

    public static void copy(QuoteLine source, QuoteLine target) {
        target.setCatalogSourceId(source.getCatalogSourceId());
        target.setCatalogSourceCode(source.getCatalogSourceCode());
        target.setCatalogSourceRevision(source.getCatalogSourceRevision());
    }
}
