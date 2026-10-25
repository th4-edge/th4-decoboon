package com.th4.edge.decoboon.supply.repository;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 合作方价格仓储。 */
public interface PartnerPriceRepository extends JpaRepository<PartnerPrice, Long> {
    List<PartnerPrice> findByCompany_ComIdOrderByPartnerPriceIdDesc(Long comId);
    PartnerPrice findFirstByPartnerAndItemAndActiveTrueOrderByPartnerPriceIdDesc(Partner partner, CatalogItem item);
}
