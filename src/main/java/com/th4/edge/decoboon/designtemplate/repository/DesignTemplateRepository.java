package com.th4.edge.decoboon.designtemplate.repository;

import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DesignTemplateRepository extends JpaRepository<DesignTemplate, Long>,
        JpaSpecificationExecutor<DesignTemplate> {

    Optional<DesignTemplate> findByTemplateIdAndCompany_ComId(Long templateId, Long comId);

    Page<DesignTemplate> findByCompany_ComIdAndStatus(Long comId, DesignTemplate.Status status, Pageable pageable);

    Page<DesignTemplate> findByCompany_ComIdAndScopeAndStatus(Long comId, DesignTemplate.Scope scope, DesignTemplate.Status status, Pageable pageable);

    Page<DesignTemplate> findByCompany_ComIdAndOwner_EmpIdAndStatus(Long comId, Long empId, DesignTemplate.Status status, Pageable pageable);
}
