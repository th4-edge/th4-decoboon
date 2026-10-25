package com.th4.edge.decoboon.supply.service.partnerprice.save.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 维护劳务/班组价格；供应商材料采购价已停止在此维护。 */
@Note("维护劳务或班组价格")
@Properties(allow = {
        @AllowProperties(model = PartnerPrice.class,
                properties = {"partnerPriceId", "partner", "item", "purchasePrice", "laborPrice",
                        "minimumOrder", "leadTimeDays", "settlementDays", "effectiveFrom", "effectiveTo",
                        "active", "remark", "createTime"}),
        @AllowProperties(model = Partner.class, properties = {"partnerId", "type", "name"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "brand", "spec"})
})
@Service("partner-price/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.supply.service.partnerprice.save.Save {
    @User private Employee employee;
    @AutoInject private Long partnerPriceId;
    @AutoInject @NotNull private Long partnerId;
    @AutoInject @NotNull private Long itemId;
    @AutoInject private BigDecimal laborPrice;
    @AutoInject private BigDecimal minimumOrder;
    @AutoInject private Integer leadTimeDays;
    @AutoInject private Integer settlementDays;
    @AutoInject private LocalDate effectiveFrom;
    @AutoInject private LocalDate effectiveTo;
    @AutoInject private Boolean active;
    @AutoInject private String remark;
    private final TenantContext tenantContext;
    private final PartnerRepository partnerRepository;
    private final CatalogItemRepository itemRepository;
    private final PartnerPriceRepository priceRepository;
    private final ModelIdentityContext modelIdentityContext;

    public Save(TenantContext tenantContext, PartnerRepository partnerRepository,
                CatalogItemRepository itemRepository, PartnerPriceRepository priceRepository,
                ModelIdentityContext modelIdentityContext) {
        this.tenantContext = tenantContext;
        this.partnerRepository = partnerRepository;
        this.itemRepository = itemRepository;
        this.priceRepository = priceRepository;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public PartnerPrice call(PartnerPrice request) {
        Employee current = tenantContext.requireRole(employee, EmployeeRole.Boss,
                EmployeeRole.StoreManager, EmployeeRole.Finance);
        Long comId = current.getCompany().getComId();
        Partner partner = partnerRepository.findById(partnerId)
                .filter(item -> item.getCompany().getComId().equals(comId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合作方不存在")));
        CatalogItem item = itemRepository.findById(itemId)
                .filter(value -> value.getCompany().getComId().equals(comId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目库项目不存在")));
        PartnerPrice price = partnerPriceId == null ? new PartnerPrice()
                : priceRepository.findById(partnerPriceId)
                .filter(value -> value.getCompany().getComId().equals(comId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "采购价记录不存在")));
        if (value(laborPrice).signum() < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "人工价不能为负数"));
        }
        price.setCompany(current.getCompany());
        price.setPartner(partner);
        price.setItem(item);
        // purchasePrice 为供应商旧价格的历史字段：新增不写入，更新保持原值。
        price.setLaborPrice(value(laborPrice));
        price.setMinimumOrder(minimumOrder == null ? BigDecimal.ONE : minimumOrder);
        price.setLeadTimeDays(leadTimeDays == null ? 0 : leadTimeDays);
        price.setSettlementDays(settlementDays == null ? 0 : settlementDays);
        price.setEffectiveFrom(effectiveFrom);
        price.setEffectiveTo(effectiveTo);
        price.setActive(active == null || active);
        price.setRemark(remark);
        return priceRepository.save(modelIdentityContext.prepare(price));
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
