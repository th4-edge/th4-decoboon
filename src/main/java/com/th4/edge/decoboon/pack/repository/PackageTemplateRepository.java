package com.th4.edge.decoboon.pack.repository;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

public interface PackageTemplateRepository extends JpaRepository<PackageTemplate, Long> {

    java.util.Optional<PackageTemplate> findByTemplateIdAndCompany_ComId(Long templateId, Long comId);

    java.util.List<PackageTemplate> findByCompany_ComIdOrderByTemplateIdDesc(Long comId);
}
