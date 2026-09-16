package com.th4.edge.decoboon.supply.repository;

import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 合作方仓储。 */
public interface PartnerRepository extends JpaRepository<Partner, Long> {
    List<Partner> findByCompany_ComIdOrderByPartnerIdDesc(Long comId);
    List<Partner> findByCompany_ComIdAndTypeAndStatusOrderByName(Long comId, PartnerType type, PartnerStatus status);
    Partner findByCompany_ComIdAndName(Long comId, String name);
}
