package com.th4.edge.decoboon.supply.repository;

import com.th4.edge.decoboon.supply.model.MaterialSupplier;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 材料-供应商供应关系仓储。 */
public interface MaterialSupplierRepository extends JpaRepository<MaterialSupplier, Long>,
        JpaSpecificationExecutor<MaterialSupplier> {

    List<MaterialSupplier> findByCompany_ComIdOrderByMaterialSupplierIdDesc(Long comId);

    List<MaterialSupplier> findByCompany_ComIdAndMaterialKeyAndActiveTrueOrderByMaterialSupplierIdDesc(
            Long comId, String materialKey);

    List<MaterialSupplier> findByCompany_ComIdAndPartner_PartnerIdOrderByMaterialSupplierIdDesc(
            Long comId, Long partnerId);

    Optional<MaterialSupplier> findFirstByCompany_ComIdAndMaterialKeyAndPartner_PartnerId(
            Long comId, String materialKey, Long partnerId);
}
