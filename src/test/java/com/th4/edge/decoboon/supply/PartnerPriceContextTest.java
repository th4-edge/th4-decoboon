package com.th4.edge.decoboon.supply;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.supply.context.PartnerPriceContext;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 供应商采购价的有效期、标准成本与敏感价格边界。 */
class PartnerPriceContextTest {

    @Test
    void shouldUseLowestEffectivePurchasePlusLaborCostAsCatalogCost() {
        PartnerPriceRepository prices = mock(PartnerPriceRepository.class);
        CatalogItemRepository items = mock(CatalogItemRepository.class);
        PartnerPriceContext context = new PartnerPriceContext(prices, items);

        Company company = new Company();
        company.setComId(1L);
        CatalogItem item = new CatalogItem();
        item.setItemId(2L);
        item.setCompany(company);

        PartnerPrice lower = price(item, "68.00", "10.00", true, null, null);
        PartnerPrice higher = price(item, "70.00", "20.00", true, null, null);
        PartnerPrice expired = price(item, "1.00", "0", true, null, LocalDate.now().minusDays(1));
        PartnerPrice inactive = price(item, "2.00", "0", false, null, null);
        when(prices.findByCompany_ComIdAndItem_ItemIdOrderByPartnerPriceIdDesc(1L, 2L))
                .thenReturn(List.of(lower, higher, expired, inactive));

        assertThat(context.standardCost(item)).isEqualByComparingTo("78.00");
        assertThat(context.effectiveCost(lower)).isEqualByComparingTo("78.00");
        assertThat(context.isEffective(expired)).isFalse();
        assertThat(context.isEffective(inactive)).isFalse();
    }

    @Test
    void shouldIgnoreFutureEffectivePrice() {
        PartnerPriceRepository prices = mock(PartnerPriceRepository.class);
        PartnerPriceContext context = new PartnerPriceContext(prices, mock(CatalogItemRepository.class));
        PartnerPrice future = price(null, "20.00", "5.00", true, LocalDate.now().plusDays(1), null);

        assertThat(context.isEffective(future)).isFalse();
        assertThat(context.effectiveCost(future)).isEqualByComparingTo("0");
    }

    private PartnerPrice price(CatalogItem item, String purchase, String labor, boolean active,
                               LocalDate from, LocalDate to) {
        PartnerPrice price = new PartnerPrice();
        price.setItem(item);
        price.setPurchasePrice(new BigDecimal(purchase));
        price.setLaborPrice(new BigDecimal(labor));
        price.setActive(active);
        price.setEffectiveFrom(from);
        price.setEffectiveTo(to);
        return price;
    }
}
