package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceiptStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 统计汇总的聚合取值（DEC-036 正模式）：一次 SQL 返回标量，不加载实体。
 *
 * <p>数据范围复用 {@link FinanceAccessContext#visible} 的同一个 Specification，
 * 权限语义与实体查询路径完全一致；逾期判定用 SQL {@code CASE WHEN} 下推，避免 Java 逐行计算。</p>
 *
 * <p>已实现：<b>应收组</b>（应收/已收/未收/逾期未收）、<b>收款组</b>和<b>付款组</b>。
 * 其余分组（项目、应付、流水、期初）按同一模式逐组实现并逐组对账。</p>
 */
@Component
public class FinanceSummaryAggregateContext {

    private final EntityManager entityManager;
    private final FinanceAccessContext accessContext;

    public FinanceSummaryAggregateContext(EntityManager entityManager, FinanceAccessContext accessContext) {
        this.entityManager = entityManager;
        this.accessContext = accessContext;
    }

    /** 应收组标量：应收合计 / 已收合计 / 未收合计 / 逾期未收合计。 */
    public ReceivableAggregate receivables(Employee current) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Receivable> root = query.from(Receivable.class);

        Expression<BigDecimal> amount = cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
        Expression<BigDecimal> received = cb.coalesce(root.<BigDecimal>get("receivedAmount"), BigDecimal.ZERO);
        Expression<BigDecimal> outstanding = cb.diff(amount, received);
        Expression<LocalDate> dueDate = root.get("dueDate");
        Expression<Object> status = root.get("status");
        Expression<BigDecimal> overdue = cb.<BigDecimal>selectCase()
                .when(cb.and(cb.lessThan(dueDate, LocalDate.now()),
                        cb.not(status.in(ReceivableStatus.Received, ReceivableStatus.Closed))),
                        outstanding)
                .otherwise(BigDecimal.ZERO);
        query.multiselect(cb.sum(amount), cb.sum(received), cb.sum(outstanding), cb.sum(overdue));
        // 权限：复用与实体查询完全相同的 Specification
        // 口径：同时排除历史归档项目（Carryover + Settled），与实体路径的 Java 侧过滤一致
        query.where(cb.and(
                accessContext.visible(current, Receivable.class).toPredicate(root, query, cb),
                cb.not(cb.and(
                        cb.equal(root.get("project").get("businessOrigin"), BusinessOrigin.Carryover),
                        cb.equal(root.get("project").get("status"), ProjectStatus.Settled)))));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new ReceivableAggregate(money(row[0]), money(row[1]), money(row[2]), money(row[3]));
    }

    /** 应收计划筛选标量：节点数 / 应收 / 已收 / 待收 / 逾期未收。 */
    public ReceivableFilteredAggregate receivables(Employee current, ReceivableCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Receivable> root = query.from(Receivable.class);
        Expression<BigDecimal> amount = cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
        Expression<BigDecimal> received = cb.coalesce(root.<BigDecimal>get("receivedAmount"), BigDecimal.ZERO);
        Expression<BigDecimal> outstanding = cb.<BigDecimal>selectCase()
                .when(cb.greaterThan(amount, received), cb.diff(amount, received))
                .otherwise(BigDecimal.ZERO);
        Expression<Object> status = root.get("status");
        Expression<LocalDate> dueDate = root.get("dueDate");
        Expression<BigDecimal> overdue = cb.<BigDecimal>selectCase()
                .when(cb.and(cb.lessThan(dueDate, LocalDate.now()),
                        cb.not(status.in(ReceivableStatus.Received, ReceivableStatus.Closed))), outstanding)
                .otherwise(BigDecimal.ZERO);
        query.multiselect(cb.count(root), cb.sum(amount), cb.sum(received), cb.sum(outstanding), cb.sum(overdue));
        query.where(cb.and(receivablePredicates(cb, query, root, current, criteria).toArray(Predicate[]::new)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new ReceivableFilteredAggregate(count(row[0]), money(row[1]), money(row[2]), money(row[3]),
                money(row[4]), LocalDateTime.now());
    }

    /** 客户退款筛选标量：笔数 / 退款总额。 */
    public RefundAggregate refunds(Employee current, RefundCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<ReceiptRefund> root = query.from(ReceiptRefund.class);
        Expression<BigDecimal> amount = cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
        query.multiselect(cb.count(root), cb.sum(amount));
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, ReceiptRefund.class).toPredicate(root, query, cb));
        if (criteria.projectId() != null) {
            predicates.add(cb.equal(root.get("project").get("projectId"), criteria.projectId()));
        }
        if (!StoreScope.isAllStores(criteria.storeFilter())) {
            predicates.add(cb.equal(root.get("store").get("storeId"), criteria.storeFilter()));
        }
        query.where(cb.and(predicates.toArray(Predicate[]::new)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new RefundAggregate(count(row[0]), money(row[1]), LocalDateTime.now());
    }

    /** 应付筛选标量：笔数 / 应付 / 已付 / 待付 / 紧急待付。 */
    public PayableAggregate payables(Employee current, PayableCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Payable> root = query.from(Payable.class);
        Expression<BigDecimal> amount = cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
        Expression<BigDecimal> paid = cb.coalesce(root.<BigDecimal>get("paidAmount"), BigDecimal.ZERO);
        Expression<BigDecimal> outstanding = cb.<BigDecimal>selectCase()
                .when(cb.greaterThan(amount, paid), cb.diff(amount, paid))
                .otherwise(BigDecimal.ZERO);
        Expression<BigDecimal> urgent = cb.<BigDecimal>selectCase()
                .when(cb.or(cb.equal(root.get("status"), PayableStatus.Overdue), cb.isTrue(root.get("urgent"))), outstanding)
                .otherwise(BigDecimal.ZERO);
        query.multiselect(cb.count(root), cb.sum(amount), cb.sum(paid), cb.sum(outstanding), cb.sum(urgent));
        query.where(cb.and(payablePredicates(cb, query, root, current, criteria).toArray(Predicate[]::new)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new PayableAggregate(count(row[0]), money(row[1]), money(row[2]), money(row[3]), money(row[4]),
                LocalDateTime.now());
    }

    /** 资金台账筛选标量：笔数 / 金额绝对值 / 收入 / 支出 / 净现金流。 */
    public LedgerAggregate ledgers(Employee current, LedgerCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Ledger> root = query.from(Ledger.class);
        Expression<BigDecimal> amount = cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
        Expression<BigDecimal> income = cb.<BigDecimal>selectCase()
                .when(cb.equal(root.get("direction"), LedgerDirection.Income), amount)
                .otherwise(BigDecimal.ZERO);
        Expression<BigDecimal> expense = cb.<BigDecimal>selectCase()
                .when(cb.equal(root.get("direction"), LedgerDirection.Expense), amount)
                .otherwise(BigDecimal.ZERO);
        query.multiselect(cb.count(root), cb.sum(amount), cb.sum(income), cb.sum(expense));
        query.where(cb.and(ledgerPredicates(cb, query, root, current, criteria).toArray(Predicate[]::new)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        BigDecimal incomeAmount = money(row[2]);
        BigDecimal expenseAmount = money(row[3]);
        return new LedgerAggregate(count(row[0]), money(row[1]), incomeAmount, expenseAmount,
                incomeAmount.subtract(expenseAmount), LocalDateTime.now());
    }

    private List<Predicate> receivablePredicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Receivable> root,
                                                 Employee current, ReceivableCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, Receivable.class).toPredicate(root, query, cb));
        if (criteria.projectId() != null) {
            predicates.add(cb.equal(root.get("project").get("projectId"), criteria.projectId()));
        }
        if (!StoreScope.isAllStores(criteria.storeFilter())) {
            predicates.add(cb.equal(root.get("store").get("storeId"), criteria.storeFilter()));
        }
        if (criteria.type() != null && !criteria.type().isBlank()) {
            predicates.add(enumEquals(cb, root.get("type"), ReceivableType.class, criteria.type()));
        }
        if (criteria.status() != null && !criteria.status().isBlank()) {
            predicates.add(enumEquals(cb, root.get("status"), ReceivableStatus.class, criteria.status()));
        }
        String keywordPattern = keywordPattern(criteria.keyword());
        if (keywordPattern != null) {
            predicates.add(cb.or(
                    contains(cb, root.get("project").get("name"), keywordPattern),
                    contains(cb, root.get("project").get("no"), keywordPattern),
                    contains(cb, root.get("name"), keywordPattern),
                    contains(cb, root.get("stageName"), keywordPattern)));
        }
        return predicates;
    }

    private List<Predicate> payablePredicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Payable> root,
                                              Employee current, PayableCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, Payable.class).toPredicate(root, query, cb));
        if (criteria.projectId() != null) {
            predicates.add(cb.equal(root.get("project").get("projectId"), criteria.projectId()));
        }
        if (!StoreScope.isAllStores(criteria.storeFilter())) {
            predicates.add(cb.equal(root.get("store").get("storeId"), criteria.storeFilter()));
        }
        if (criteria.partnerId() != null) {
            predicates.add(cb.equal(root.get("payeePartner").get("partnerId"), criteria.partnerId()));
        }
        if (criteria.type() != null && !criteria.type().isBlank()) {
            predicates.add(enumEquals(cb, root.get("type"), PayableType.class, criteria.type()));
        }
        if (criteria.status() != null && !criteria.status().isBlank()) {
            predicates.add(enumEquals(cb, root.get("status"), PayableStatus.class, criteria.status()));
        }
        if (criteria.urgent() != null) {
            predicates.add(cb.equal(root.get("urgent"), criteria.urgent()));
        }
        String keywordPattern = keywordPattern(criteria.keyword());
        if (keywordPattern != null) {
            Join<Payable, Project> project = root.join("project", JoinType.LEFT);
            predicates.add(cb.or(
                    contains(cb, root.get("name"), keywordPattern),
                    contains(cb, root.get("payeeName"), keywordPattern),
                    contains(cb, project.get("name"), keywordPattern)));
        }
        return predicates;
    }

    private List<Predicate> ledgerPredicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Ledger> root,
                                             Employee current, LedgerCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, Ledger.class).toPredicate(root, query, cb));
        predicates.add(cb.isFalse(root.get("voided")));
        if (criteria.projectId() != null) {
            predicates.add(cb.equal(root.get("project").get("projectId"), criteria.projectId()));
        }
        if (!StoreScope.isAllStores(criteria.storeFilter())) {
            predicates.add(cb.equal(root.get("store").get("storeId"), criteria.storeFilter()));
        }
        if (criteria.ledgerIds() != null && !criteria.ledgerIds().isEmpty()) {
            predicates.add(root.get("ledgerId").in(criteria.ledgerIds()));
        }
        if (criteria.direction() != null && !criteria.direction().isBlank()) {
            predicates.add(enumEquals(cb, root.get("direction"), LedgerDirection.class, criteria.direction()));
        }
        if (criteria.categoryId() != null) {
            predicates.add(cb.equal(root.get("categoryNode").get("categoryId"), criteria.categoryId()));
        }
        if (criteria.category() != null && !criteria.category().isBlank()) {
            predicates.add(cb.equal(root.get("category"), criteria.category()));
        }
        if (Boolean.TRUE.equals(criteria.uncategorized())) {
            predicates.add(cb.and(
                    cb.equal(root.get("source"), LedgerSource.Manual),
                    cb.isNull(root.get("categoryNode"))));
        }
        if (criteria.startDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), criteria.startDate()));
        }
        if (criteria.endDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), criteria.endDate()));
        }
        String keywordPattern = keywordPattern(criteria.keyword());
        if (keywordPattern != null) {
            Join<Ledger, LedgerCategory> category = root.join("categoryNode", JoinType.LEFT);
            predicates.add(cb.or(
                    contains(cb, root.get("summary"), keywordPattern),
                    contains(cb, root.get("counterparty"), keywordPattern),
                    contains(cb, root.get("category"), keywordPattern),
                    contains(cb, category.get("name"), keywordPattern)));
        }
        return predicates;
    }

    private <E extends Enum<E>> Predicate enumEquals(CriteriaBuilder cb, Path<?> path,
                                                     Class<E> enumType, String rawValue) {
        E parsed = enumValue(enumType, rawValue);
        return parsed == null ? cb.disjunction() : cb.equal(path, parsed);
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumType, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        for (E value : enumType.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(rawValue.trim())) {
                return value;
            }
        }
        return null;
    }

    /** 收款组标量：笔数 / 收款总额 / 已冲正金额 / 有效收款金额。 */
    public ReceiptAggregate receipts(Employee current, ReceiptCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Receipt> root = query.from(Receipt.class);
        Expression<BigDecimal> amount = cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
        Expression<BigDecimal> reversed = cb.<BigDecimal>selectCase()
                .when(cb.equal(root.get("status"), ReceiptStatus.Reversed), amount)
                .otherwise(BigDecimal.ZERO);
        query.multiselect(cb.count(root), cb.sum(amount), cb.sum(reversed));
        query.where(cb.and(receiptPredicates(cb, query, root, current, criteria).toArray(Predicate[]::new)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        BigDecimal total = money(row[1]);
        BigDecimal reversedAmount = money(row[2]);
        return new ReceiptAggregate(count(row[0]), total, reversedAmount, total.subtract(reversedAmount), LocalDateTime.now());
    }

    /** 付款组标量：笔数 / 付款总额。 */
    public PaymentAggregate payments(Employee current, PaymentCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Payment> root = query.from(Payment.class);
        Expression<BigDecimal> amount = cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
        query.multiselect(cb.count(root), cb.sum(amount));
        query.where(cb.and(paymentPredicates(cb, query, root, current, criteria).toArray(Predicate[]::new)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new PaymentAggregate(count(row[0]), money(row[1]), LocalDateTime.now());
    }

    private List<Predicate> receiptPredicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Receipt> root,
                                              Employee current, ReceiptCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, Receipt.class).toPredicate(root, query, cb));
        if (criteria.projectId() != null) {
            predicates.add(cb.equal(root.get("project").get("projectId"), criteria.projectId()));
        }
        if (!StoreScope.isAllStores(criteria.storeFilter())) {
            predicates.add(cb.equal(root.get("store").get("storeId"), criteria.storeFilter()));
        }
        String keywordPattern = keywordPattern(criteria.keyword());
        if (keywordPattern != null) {
            predicates.add(cb.or(
                    contains(cb, root.get("project").get("name"), keywordPattern),
                    contains(cb, root.get("project").get("no"), keywordPattern),
                    contains(cb, root.get("payerName"), keywordPattern),
                    contains(cb, root.get("remark"), keywordPattern)));
        }
        return predicates;
    }

    private List<Predicate> paymentPredicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Payment> root,
                                              Employee current, PaymentCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, Payment.class).toPredicate(root, query, cb));
        if (criteria.projectId() != null) {
            predicates.add(cb.equal(root.get("project").get("projectId"), criteria.projectId()));
        }
        if (!StoreScope.isAllStores(criteria.storeFilter())) {
            predicates.add(cb.equal(root.get("store").get("storeId"), criteria.storeFilter()));
        }
        String keywordPattern = keywordPattern(criteria.keyword());
        if (keywordPattern != null) {
            Join<Payment, Project> project = root.join("project", JoinType.LEFT);
            predicates.add(cb.or(
                    contains(cb, root.get("payeeName"), keywordPattern),
                    contains(cb, root.get("payable").get("name"), keywordPattern),
                    contains(cb, root.get("transactionNo"), keywordPattern),
                    contains(cb, root.get("remark"), keywordPattern),
                    contains(cb, project.get("name"), keywordPattern),
                    contains(cb, project.get("no"), keywordPattern)));
        }
        return predicates;
    }

    private Predicate contains(CriteriaBuilder cb, Path<String> path, String pattern) {
        return cb.like(cb.lower(cb.coalesce(path, "")), pattern);
    }

    private String keywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    private Integer count(Object value) {
        return value == null ? 0 : Math.toIntExact(((Number) value).longValue());
    }

    private BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
    }

    /** 应收组标量结果。 */
    public record ReceivableAggregate(BigDecimal amount, BigDecimal received,
                                      BigDecimal outstanding, BigDecimal overdue) { }

    /** 应收计划筛选条件。 */
    public record ReceivableCriteria(Long projectId, String type, String status, String keyword, Long storeFilter) { }

    /** 应收计划筛选结果。 */
    public record ReceivableFilteredAggregate(Integer count, BigDecimal totalAmount, BigDecimal receivedAmount,
                                              BigDecimal outstandingAmount, BigDecimal overdueAmount,
                                              LocalDateTime asOf) { }

    /** 客户退款筛选条件。 */
    public record RefundCriteria(Long projectId, Long storeFilter) { }

    /** 客户退款筛选结果。 */
    public record RefundAggregate(Integer count, BigDecimal totalAmount, LocalDateTime asOf) { }

    /** 应付筛选条件。 */
    public record PayableCriteria(Long projectId, String type, String status, Boolean urgent, Long partnerId,
                                  String keyword, Long storeFilter) { }

    /** 应付筛选结果。 */
    public record PayableAggregate(Integer count, BigDecimal totalAmount, BigDecimal paidAmount,
                                   BigDecimal outstandingAmount, BigDecimal urgentAmount, LocalDateTime asOf) { }

    /** 资金台账筛选条件。 */
    public record LedgerCriteria(Long projectId, String direction, Long categoryId, String category,
                                 Boolean uncategorized, LocalDate startDate, LocalDate endDate,
                                 String keyword, Collection<Long> ledgerIds, Long storeFilter) { }

    /** 资金台账筛选结果。 */
    public record LedgerAggregate(Integer count, BigDecimal totalAmount, BigDecimal incomeAmount,
                                  BigDecimal expenseAmount, BigDecimal netCashFlowAmount,
                                  LocalDateTime asOf) { }

    /** 收款组筛选条件。 */
    public record ReceiptCriteria(Long projectId, String keyword, Long storeFilter) { }

    /** 收款组标量结果。 */
    public record ReceiptAggregate(Integer count, BigDecimal totalAmount, BigDecimal reversedAmount,
                                   BigDecimal netAmount, LocalDateTime asOf) { }

    /** 付款组筛选条件。 */
    public record PaymentCriteria(Long projectId, String keyword, Long storeFilter) { }

    /** 付款组标量结果。 */
    public record PaymentAggregate(Integer count, BigDecimal totalAmount, LocalDateTime asOf) { }
}
