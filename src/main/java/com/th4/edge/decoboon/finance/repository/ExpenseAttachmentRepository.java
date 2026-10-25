package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.ExpenseAttachment;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 费用证据附件仓储。 */
public interface ExpenseAttachmentRepository extends JpaRepository<ExpenseAttachment, Long> {
    List<ExpenseAttachment> findByCompany_ComIdAndExpense_ExpenseIdOrderByAttachmentIdAsc(Long comId, Long expenseId);
}
