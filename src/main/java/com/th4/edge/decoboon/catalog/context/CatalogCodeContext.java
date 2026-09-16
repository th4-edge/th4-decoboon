package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 报价项目编号生成。
 *
 * <p>规则：{类型前缀}-{企业内 4 位序号}，例如 施工 SG-0001、主材 ZC-0001。
 * 序号按「企业 + 类型」独立递增，兼容不同企业的自定义命名（前端仍可手填覆盖）。</p>
 */
@Component
public class CatalogCodeContext {

    private static final Map<ItemType, String> PREFIX = new EnumMap<>(ItemType.class);
    private static final String FALLBACK = "QT";
    private static final int WIDTH = 4;

    static {
        PREFIX.put(ItemType.Construction, "SG");
        PREFIX.put(ItemType.Material, "ZC");
        PREFIX.put(ItemType.Soft, "RZ");
        PREFIX.put(ItemType.Appliance, "DQ");
        PREFIX.put(ItemType.General, "ZH");
        PREFIX.put(ItemType.Custom, "DZ");
    }

    private final CatalogItemRepository repository;

    public CatalogCodeContext(CatalogItemRepository repository) {
        this.repository = repository;
    }

    /** 生成下一个可用编号；同企业同类型内单调递增。 */
    @Transactional(readOnly = true)
    public String nextCode(Long comId, ItemType type) {
        String prefix = PREFIX.getOrDefault(type, FALLBACK);
        Optional<CatalogItem> latest =
                repository.findFirstByCompany_ComIdAndTypeOrderByCodeDesc(comId, type);
        int next = latest.map(item -> sequence(item.getCode(), prefix) + 1).orElse(1);
        return String.format("%s-%0" + WIDTH + "d", prefix, next);
    }

    /** 解析既有编号中的序号；无法解析（含用户自定义编号）时回退为 0。 */
    private int sequence(String code, String prefix) {
        if (code == null || code.isBlank()) {
            return 0;
        }
        String tail = code;
        int dash = code.lastIndexOf('-');
        if (dash >= 0) {
            tail = code.substring(dash + 1);
        }
        try {
            return Integer.parseInt(tail.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
