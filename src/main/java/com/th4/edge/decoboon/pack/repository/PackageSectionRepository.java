package com.th4.edge.decoboon.pack.repository;

import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

public interface PackageSectionRepository extends JpaRepository<PackageSection, Long> {

    java.util.Optional<PackageSection> findByGroupIdAndCompany_ComId(Long groupId, Long comId);

    java.util.List<PackageSection> findByCompany_ComIdOrderByPriorityAscGroupIdAsc(Long comId);
}
