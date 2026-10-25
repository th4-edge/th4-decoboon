package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 费用/报销仓储。 */
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByCompany_ComIdAndIdempotencyKey(Long comId, String idempotencyKey);

    Optional<Expense> findByCompany_ComIdAndExpenseId(Long comId, Long expenseId);

    /** 项目成本投影：只取已批准/已关闭的项目归属费用，企业费用由 project 为空自然排除。 */
    @EntityGraph(attributePaths = {"project"})
    List<Expense> findByProject_ProjectIdInAndStatusIn(Collection<Long> projectIds,
                                                       Collection<ExpenseStatus> statuses);

    /** 详情主查询只预取页面直接展示的对象；人员字段由下方窄查询补齐，避免单条重 JOIN。 */
    @EntityGraph(attributePaths = {"category", "project", "store", "attachments.uploadedBy"})
    Optional<Expense> findDetailByCompany_ComIdAndExpenseId(Long comId, Long expenseId);

    @EntityGraph(attributePaths = {"requester", "owner", "payeeEmployee", "approvedBy"})
    @Query("select e from Expense e where e.expenseId in :ids")
    List<Expense> findDetailPeopleByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"category", "project", "store", "requester"})
    @Query("select e from Expense e where e.expenseId in :ids")
    List<Expense> findPageByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"category", "project", "store", "payable"})
    Optional<Expense> findVoucherSourceByCompany_ComIdAndExpenseId(Long comId, Long expenseId);

    @EntityGraph(attributePaths = {"requester", "owner", "payeeEmployee", "approvedBy"})
    @Query("select e from Expense e where e.expenseId in :ids")
    List<Expense> findVoucherPeopleByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"owner", "payeeEmployee", "approvedBy"})
    @Query("select e from Expense e where e.expenseId in :ids")
    List<Expense> findPagePeopleByIdIn(@Param("ids") Collection<Long> ids);
}
