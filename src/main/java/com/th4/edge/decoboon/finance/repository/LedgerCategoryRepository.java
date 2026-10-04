package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 手工记账分类仓储。 */
public interface LedgerCategoryRepository extends JpaRepository<LedgerCategory, Long>,
        JpaSpecificationExecutor<LedgerCategory> {

    List<LedgerCategory> findByCompany_ComIdOrderByDirectionAscSortAscCategoryIdAsc(Long comId);

    boolean existsByCompany_ComIdAndBuiltInTrue(Long comId);

    Optional<LedgerCategory> findByCompany_ComIdAndCategoryId(Long comId, Long categoryId);

    List<LedgerCategory> findByCompany_ComIdAndBuiltInTrue(Long comId);

    List<LedgerCategory> findByCompany_ComIdAndParent_CategoryIdAndDirectionAndActiveTrueOrderBySortAscCategoryIdAsc(
            Long comId, Long parentId, LedgerDirection direction);

    boolean existsByCompany_ComIdAndDirectionAndParent_CategoryIdAndName(
            Long comId, LedgerDirection direction, Long parentId, String name);

    boolean existsByCompany_ComIdAndDirectionAndParentIsNullAndName(
            Long comId, LedgerDirection direction, String name);

    List<LedgerCategory> findByCompany_ComIdAndParent_CategoryId(Long comId, Long parentId);
}
