package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 费用分类仓储。 */
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long>,
        JpaSpecificationExecutor<ExpenseCategory> {

    List<ExpenseCategory> findByCompany_ComIdOrderBySortAscCategoryIdAsc(Long comId);

    List<ExpenseCategory> findByCompany_ComIdAndBuiltInTrue(Long comId);

    Optional<ExpenseCategory> findByCompany_ComIdAndCategoryId(Long comId, Long categoryId);

    boolean existsByCompany_ComIdAndCode(Long comId, String code);
}
