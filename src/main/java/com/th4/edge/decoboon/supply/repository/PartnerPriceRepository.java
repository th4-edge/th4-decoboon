package com.th4.edge.decoboon.supply.repository;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/** 合作方价格仓储。 */
public interface PartnerPriceRepository extends JpaRepository<PartnerPrice, Long>,
        JpaSpecificationExecutor<PartnerPrice> {
    @Override
    @EntityGraph(attributePaths = {"partner", "item"})
    Page<PartnerPrice> findAll(Specification<PartnerPrice> specification, Pageable pageable);

    List<PartnerPrice> findByCompany_ComIdOrderByPartnerPriceIdDesc(Long comId);
    PartnerPrice findFirstByPartnerAndItemAndActiveTrueOrderByPartnerPriceIdDesc(Partner partner, CatalogItem item);
}
