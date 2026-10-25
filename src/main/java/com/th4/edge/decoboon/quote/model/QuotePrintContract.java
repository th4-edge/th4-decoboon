package com.th4.edge.decoboon.quote.model;

import com.th4.edge.decoboon.quote.configure.QuoteViewMode;
import com.th4.edge.framework.core.annotation.Note;

import java.util.List;
import java.util.Set;

/** 报价 PDF 的版本、分组和客户字段白名单契约。 */
@Note("报价 PDF 契约")
public final class QuotePrintContract {

    public static final List<String> REQUIRED_PARAMETERS = List.of("quoteId", "version", "groupMode");
    public static final QuoteViewMode DEFAULT_GROUP_MODE = QuoteViewMode.REGION;

    public static final List<String> CUSTOMER_VISIBLE_FIELDS = List.of(
            "no", "version", "status", "acceptedAt", "customer", "property",
            "groupMode", "groupName", "category", "name", "brand", "spec", "unit", "quantity",
            "price", "amount", "role", "differenceStatus", "included", "craft", "componentSales",
            "subtotal", "discount", "taxRate", "taxAmount", "amount"
    );

    public static final List<String> FORBIDDEN_FIELDS = List.of(
            "cost", "costComplete", "profit", "grossMargin", "componentCost", "actualCost",
            "formula", "quantityFormula", "supplierPrice", "purchasePrice", "acceptedBy"
    );

    private QuotePrintContract() {
    }

    public static void requireParameters(Long quoteId, Integer version, QuoteViewMode groupMode) {
        if (quoteId == null || version == null || groupMode == null) {
            throw new IllegalArgumentException("PDF 必须绑定 quoteId、version 和 groupMode");
        }
        if (!Set.of(QuoteViewMode.REGION, QuoteViewMode.TYPE).contains(groupMode)) {
            throw new IllegalArgumentException("不支持的 PDF 分组方式：" + groupMode);
        }
    }

    public static String fileSuffix(Integer version, QuoteViewMode groupMode) {
        if (version == null || groupMode == null) {
            throw new IllegalArgumentException("version 和 groupMode 不能为空");
        }
        return "_V" + version + "_" + groupMode.getName();
    }

}
