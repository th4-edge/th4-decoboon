package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.CommissionStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.configure.PayslipStatus;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.model.Payslip;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import com.th4.edge.tenant.user.model.Employee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 薪酬工作台聚合查询（DEC-036 / BR-031）。
 *
 * <p>所有指标由数据库聚合返回，只查询标量或最近一行，不加载工资项、应付、提成或工资条实体。
 * 数据范围复用 {@link PayrollAccessContext#visible} 的 Specification，门店维度复用
 * {@link StoreScope} 的哨兵语义。</p>
 */
@Component
public class PayrollWorkbenchAggregateContext {

    private final EntityManager entityManager;
    private final PayrollAccessContext accessContext;

    public PayrollWorkbenchAggregateContext(EntityManager entityManager, PayrollAccessContext accessContext) {
        this.entityManager = entityManager;
        this.accessContext = accessContext;
    }

    public AttendanceAggregate attendance(Employee current, String period, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<AttendanceSummary> root = query.from(AttendanceSummary.class);

        Expression<Long> pending = cb.sum(cb.<Long>selectCase()
                .when(root.get("status").in(AttendanceStatus.Pending, AttendanceStatus.Disputed), 1L)
                .otherwise(0L));
        query.select(cb.array(pending));
        query.where(cb.and(
                accessContext.visible(current, AttendanceSummary.class).toPredicate(root, query, cb),
                cb.equal(root.get("payrollMonth"), period),
                storePredicate(cb, root, storeFilter)));

        return new AttendanceAggregate(count(entityManager.createQuery(query).getSingleResult()[0]));
    }

    public SheetAggregate sheets(Employee current, String period, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<PayrollSheet> root = query.from(PayrollSheet.class);

        query.multiselect(
                cb.sum(statusCount(cb, root, PayrollSheetStatus.Draft)),
                cb.sum(statusCount(cb, root, PayrollSheetStatus.Pending)),
                cb.sum(statusCount(cb, root, PayrollSheetStatus.Confirmed)),
                cb.sum(statusCount(cb, root, PayrollSheetStatus.PartialPaid)),
                cb.sum(statusCount(cb, root, PayrollSheetStatus.Paid)),
                cb.count(root));
        query.where(cb.and(
                visibleSheets(current, storeFilter).toPredicate(root, query, cb),
                cb.equal(root.get("period"), period)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new SheetAggregate(count(row[0]), count(row[1]), count(row[2]), count(row[3]),
                count(row[4]), count(row[5]) > 0);
    }

    public LatestSheet latestSheet(Employee current, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<PayrollSheet> root = query.from(PayrollSheet.class);

        query.multiselect(root.get("sheetId"), root.get("period"), root.get("status"), root.get("totalNetAmount"));
        query.where(visibleSheets(current, storeFilter).toPredicate(root, query, cb));
        query.orderBy(cb.desc(root.get("period")), cb.desc(root.get("sheetId")));

        List<Object[]> rows = entityManager.createQuery(query).setMaxResults(1).getResultList();
        if (rows.isEmpty()) {
            return LatestSheet.empty();
        }
        Object[] row = rows.get(0);
        return new LatestSheet((Long) row[0], (String) row[1], (PayrollSheetStatus) row[2], money(row[3]));
    }

    public PayableAggregate payables(Employee current, String period, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Payable> root = query.from(Payable.class);

        Expression<BigDecimal> remaining = positiveRemaining(
                cb, root.<BigDecimal>get("amount"), root.<BigDecimal>get("paidAmount"));
        query.select(cb.array(cb.sum(remaining)));
        query.where(cb.and(
                accessContext.visible(current, Payable.class).toPredicate(root, query, cb),
                storePredicate(cb, root, storeFilter),
                cb.or(
                        cb.equal(root.get("type"), PayableType.EmployeeCommission),
                        cb.and(
                                cb.equal(root.get("type"), PayableType.EmployeeSalary),
                                cb.equal(root.get("period"), period)))));

        return new PayableAggregate(money(entityManager.createQuery(query).getSingleResult()[0]));
    }

    public CommissionAggregate commissions(Employee current, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<CommissionItem> root = query.from(CommissionItem.class);

        Expression<BigDecimal> remaining = positiveRemaining(
                cb, root.<BigDecimal>get("releasedAmount"), root.<BigDecimal>get("paidAmount"));
        query.select(cb.array(cb.sum(remaining)));
        query.where(cb.and(
                accessContext.visible(current, CommissionItem.class).toPredicate(root, query, cb),
                storePredicate(cb, root, storeFilter),
                root.get("status").in(CommissionStatus.Released, CommissionStatus.InPayment)));

        return new CommissionAggregate(money(entityManager.createQuery(query).getSingleResult()[0]));
    }

    public PayslipAggregate payslips(Employee current, String period, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Payslip> root = query.from(Payslip.class);

        query.multiselect(
                cb.sum(statusCount(cb, root, PayslipStatus.PendingPayment)),
                cb.sum(statusCount(cb, root, PayslipStatus.Published)));
        query.where(cb.and(
                accessContext.visible(current, Payslip.class).toPredicate(root, query, cb),
                storePredicate(cb, root, storeFilter),
                cb.equal(root.get("period"), period)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new PayslipAggregate(count(row[0]), count(row[1]));
    }

    /**
     * 工资单是按企业+期间唯一的公司级单据；单店视图的可见条件与
     * {@link PayrollAccessContext#visibleSheets(Employee, Long)} 相同，但用子查询下推，
     * 避免先查本店工资项 ID 再拼接主查询。
     */
    private Specification<PayrollSheet> visibleSheets(Employee current, Long storeFilter) {
        Long comId = current.getCompany().getComId();
        return (root, query, cb) -> {
            Predicate company = cb.equal(root.get("company").get("comId"), comId);
            if (StoreScope.isAllStores(storeFilter)) {
                return company;
            }
            Subquery<Long> itemSheetIds = query.subquery(Long.class);
            Root<PayrollItem> item = itemSheetIds.from(PayrollItem.class);
            itemSheetIds.select(item.get("sheet").get("sheetId"));
            itemSheetIds.where(
                    cb.equal(item.get("company").get("comId"), comId),
                    cb.equal(item.get("store").get("storeId"), storeFilter));
            return cb.and(company, cb.or(
                    cb.equal(root.get("store").get("storeId"), storeFilter),
                    root.get("sheetId").in(itemSheetIds)));
        };
    }

    private <T> Expression<Long> statusCount(CriteriaBuilder cb, Root<T> root, Enum<?> status) {
        return cb.<Long>selectCase().when(cb.equal(root.get("status"), status), 1L).otherwise(0L);
    }

    private Predicate storePredicate(CriteriaBuilder cb, Root<?> root, Long storeFilter) {
        return StoreScope.isAllStores(storeFilter)
                ? cb.conjunction()
                : cb.equal(root.get("store").get("storeId"), storeFilter);
    }

    private Expression<BigDecimal> positiveRemaining(CriteriaBuilder cb,
                                                     Expression<BigDecimal> amount,
                                                     Expression<BigDecimal> paid) {
        Expression<BigDecimal> remaining = cb.diff(
                cb.coalesce(amount, BigDecimal.ZERO),
                cb.coalesce(paid, BigDecimal.ZERO));
        return cb.<BigDecimal>selectCase()
                .when(cb.greaterThan(remaining, BigDecimal.ZERO), remaining)
                .otherwise(BigDecimal.ZERO);
    }

    private int count(Object value) {
        return value == null ? 0 : Math.toIntExact(((Number) value).longValue());
    }

    private BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
    }

    public record AttendanceAggregate(int pendingCount) { }

    public record SheetAggregate(int draftCount, int pendingCount, int confirmedCount,
                                 int partialPaidCount, int paidCount, boolean currentPeriodHasSheet) { }

    public record LatestSheet(Long sheetId, String period, PayrollSheetStatus status, BigDecimal totalNetAmount) {
        public static LatestSheet empty() {
            return new LatestSheet(null, null, null, BigDecimal.ZERO);
        }
    }

    public record PayableAggregate(BigDecimal pendingEmployeePayableAmount) { }

    public record CommissionAggregate(BigDecimal releasedUnpaidCommissionAmount) { }

    public record PayslipAggregate(int pendingCount, int publishedCount) { }
}
