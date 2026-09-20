package com.th4.edge.decoboon.pack.repository;

import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

public interface PackageLineRepository extends JpaRepository<PackageLine, Long> {

    java.util.Optional<PackageLine> findByItemIdAndCompany_ComId(Long itemId, Long comId);

    java.util.List<PackageLine> findByTemplate_TemplateIdOrderBySortAscItemIdAsc(Long templateId);

    long countBySection_GroupId(Long groupId);
}
