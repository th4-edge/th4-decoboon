package com.th4.edge.decoboon.supply.context;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** 供应商采购价解析与项目库标准成本同步。 */
@Component
public class PartnerPriceContext {

    private final PartnerPriceRepository priceRepository;
    private final CatalogItemRepository catalogItemRepository;

    public PartnerPriceContext(PartnerPriceRepository priceRepository,
                               CatalogItemRepository catalogItemRepository) {
        this.priceRepository = priceRepository;
        this.catalogItemRepository = catalogItemRepository;
    }

    /** 当前有效的供应商采购成本：采购价 + 人工/班组价。 */
    public BigDecimal effectiveCost(PartnerPrice price) {
        if (price == null || !isEffective(price)) {
            return BigDecimal.ZERO;
        }
        return value(price.getPurchasePrice()).add(value(price.getLaborPrice()));
    }

    /** 指定供应商与目录项的当前有效采购价。 */
    public PartnerPrice preferred(Partner partner, CatalogItem item) {
        if (partner == null || item == null) {
            return null;
        }
        return priceRepository.findByPartnerOrderByPartnerPriceIdDesc(partner).stream()
                .filter(price -> price.getItem() != null
                        && item.getItemId().equals(price.getItem().getItemId()))
                .filter(this::isEffective)
                .min(Comparator.comparing(this::effectiveCost))
                .orElse(null);
    }

    /** 当前企业、当前目录项的最小有效采购成本。 */
    public BigDecimal standardCost(CatalogItem item) {
        if (item == null || item.getItemId() == null || item.getCompany() == null) {
            return BigDecimal.ZERO;
        }
        return priceRepository.findByCompany_ComIdAndItem_ItemIdOrderByPartnerPriceIdDesc(
                        item.getCompany().getComId(), item.getItemId()).stream()
                .filter(this::isEffective)
                .map(this::effectiveCost)
                .filter(cost -> cost.signum() > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /** 采购价变化后同步目录标准成本；设计分项继续使用目录成本快照。 */
    @Transactional
    public CatalogItem refreshCatalogCost(CatalogItem item) {
        if (item == null || item.getItemId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "报价项目库项目不能为空"));
        }
        item.setCostPrice(standardCost(item));
        return catalogItemRepository.save(item);
    }

    public boolean isEffective(PartnerPrice price) {
        if (price == null || !Boolean.TRUE.equals(price.getActive())) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (price.getEffectiveFrom() != null && price.getEffectiveFrom().isAfter(today)) {
            return false;
        }
        return price.getEffectiveTo() == null || !price.getEffectiveTo().isBefore(today);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
