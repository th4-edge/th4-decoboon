package com.th4.edge.decoboon.quote.model;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.quote.configure.QuoteViewMode;
import com.th4.edge.decoboon.quote.configure.QuoteViewScene;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 报价统一视图的一期契约。
 *
 * <p>这里只冻结服务端合并身份、来源字段和场景消费规则，不执行 Region/Type 分组。
 * 实际分组由后续切片在 QuoteViewContext 中统一实现。</p>
 */
@Note("报价统一视图契约")
public final class QuoteViewContract {

    public static final List<String> TYPE_MERGE_KEY_FIELDS = List.of(
            "catalogItem.itemId",
            "type",
            "category",
            "name",
            "brand",
            "spec",
            "unit",
            "pricingMode",
            "price",
            "fixedTotalPrice",
            "role",
            "differenceType",
            "differenceStatus",
            "included",
            "discountAmount",
            "craft",
            "components.saleSignature"
    );

    public static final List<String> TYPE_SOURCE_FIELDS = List.of(
            "typeRowId",
            "sourceLineIds",
            "sourceRegionIds",
            "sourceRegionNames",
            "sourceCount"
    );

    private static final String CUSTOM_SOURCE = "CUSTOM";

    private QuoteViewContract() {
    }

    public static QuoteViewMode defaultGroupMode(QuoteViewScene scene) {
        Objects.requireNonNull(scene, "scene");
        return scene.getDefaultGroupMode();
    }

    public static boolean canUse(QuoteViewScene scene, QuoteViewMode mode) {
        Objects.requireNonNull(scene, "scene");
        return scene.supports(mode);
    }

    /**
     * 生成 TYPE 合并行的稳定 ID。
     *
     * <p>身份只依赖 quoteId、version 和规范化合并键，与区域、数量、金额、成本及数组顺序无关。</p>
     */
    public static String typeRowId(Long quoteId, Integer version, QuoteLine line) {
        if (quoteId == null || version == null || line == null) {
            throw new IllegalArgumentException("quoteId、version 和报价行不能为空");
        }
        String source = quoteId + "|" + version + "|" + canonicalMergeKey(line);
        return "quote-type-" + sha256(source).substring(0, 32);
    }

    /** 规范化后的 TYPE 合并键；相同返回值表示业务身份一致。 */
    public static String canonicalMergeKey(QuoteLine line) {
        Objects.requireNonNull(line, "line");
        return canonicalFields(
                catalogIdentity(line),
                enumKey(line.getType()),
                normalizedCategory(line),
                text(line.getName()),
                text(line.getBrand()),
                text(line.getSpec()),
                enumKey(line.getUnit()),
                enumKey(line.getPricingMode()),
                decimal(line.getPrice()),
                line.getPricingMode() == PricingMethod.FixedTotal ? decimal(line.getFixedTotalPrice()) : "",
                enumKey(line.getRole()),
                enumKey(line.getDifferenceType()),
                enumKey(line.getDifferenceStatus()),
                Boolean.toString(!Boolean.FALSE.equals(line.getIncluded())),
                decimal(line.getDiscountAmount()),
                text(line.getCraft()),
                componentSignature(line.getComponents()));
    }


    /** 分类为空时回退到 ItemType 展示名，并输出规范化比较键。 */
    public static String normalizedCategory(QuoteLine line) {
        Objects.requireNonNull(line, "line");
        String category = text(line.getCategory());
        if (!category.isEmpty()) {
            return category;
        }
        ItemType type = line.getType() == null ? ItemType.General : line.getType();
        return text(type.getName());
    }

    private static String catalogIdentity(QuoteLine line) {
        return line.getCatalogItem() == null || line.getCatalogItem().getItemId() == null
                ? CUSTOM_SOURCE
                : "CATALOG:" + line.getCatalogItem().getItemId();
    }

    private static String componentSignature(List<QuoteComponent> components) {
        List<QuoteComponent> sorted = components == null ? List.of() : components.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((QuoteComponent component) -> component.getSort() == null ? Integer.MAX_VALUE : component.getSort())
                        .thenComparing(component -> enumKey(component.getType()))
                        .thenComparing(component -> text(component.getName()))
                        .thenComparing(component -> decimal(component.getSaleAmount())))
                .toList();
        StringBuilder signature = new StringBuilder();
        appendField(signature, Integer.toString(sorted.size()));
        for (QuoteComponent component : sorted) {
            appendField(signature, enumKey(component.getType()));
            appendField(signature, text(component.getName()));
            appendField(signature, decimal(component.getSaleAmount()));
        }
        return signature.toString();
    }

    /**
     * 使用“长度 + 冒号 + 原值”的无歧义编码，避免字段值含分隔符时发生业务身份碰撞。
     */
    private static String canonicalFields(String... fields) {
        StringBuilder canonical = new StringBuilder();
        for (String field : fields) {
            appendField(canonical, field);
        }
        return canonical.toString();
    }

    private static void appendField(StringBuilder output, String value) {
        String safe = value == null ? "" : value;
        output.append(safe.length()).append(':').append(safe).append(';');
    }

    private static String enumKey(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String text(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
