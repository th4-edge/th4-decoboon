package com.th4.edge.decoboon.pack.repository;

import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface PackageTemplateRepository extends JpaRepository<PackageTemplate, Long> {

    @EntityGraph(attributePaths = {"items", "items.section", "items.item"})
    Optional<PackageTemplate> findByTemplateIdAndCompany_ComId(Long templateId, Long comId);

    List<PackageTemplate> findByCompany_ComIdOrderByCodeAscVersionDescTemplateIdDesc(Long comId);

    List<PackageTemplate> findByCompany_ComIdOrderByTemplateIdDesc(Long comId);

    List<PackageTemplate> findByCompany_ComIdAndCodeOrderByVersionDesc(Long comId, String code);

    Optional<PackageTemplate> findFirstByCompany_ComIdAndCodeOrderByVersionDesc(Long comId, String code);

    List<PackageTemplate> findByCompany_ComIdAndCodeAndCurrentPublishedTrue(Long comId, String code);

    List<PackageTemplate> findByCompany_ComIdAndStatusAndCurrentPublishedTrueOrderByCodeAsc(
            Long comId, com.th4.edge.decoboon.pack.configure.PackageTemplateStatus status);
}
