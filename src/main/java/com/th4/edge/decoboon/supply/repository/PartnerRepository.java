package com.th4.edge.decoboon.supply.repository;

import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

/** 合作方仓储。 */
public interface PartnerRepository extends JpaRepository<Partner, Long> {
    @EntityGraph(attributePaths = "brands")
    List<Partner> findByCompany_ComIdOrderByPartnerIdDesc(Long comId);
    @EntityGraph(attributePaths = "brands")
    List<Partner> findByCompany_ComIdAndTypeAndStatusOrderByName(Long comId, PartnerType type, PartnerStatus status);
    Partner findByCompany_ComIdAndName(Long comId, String name);
}
