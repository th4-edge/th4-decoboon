package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.ExpenseRevision;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

/** 费用提交版本仓储。 */
public interface ExpenseRevisionRepository extends JpaRepository<ExpenseRevision, Long> {
    @EntityGraph(attributePaths = {"submittedBy", "returnedBy"})
    List<ExpenseRevision> findByCompany_ComIdAndExpense_ExpenseIdOrderByVersionDesc(Long comId, Long expenseId);

    Optional<ExpenseRevision> findByCompany_ComIdAndExpense_ExpenseIdAndVersion(Long comId, Long expenseId, Integer version);
}
