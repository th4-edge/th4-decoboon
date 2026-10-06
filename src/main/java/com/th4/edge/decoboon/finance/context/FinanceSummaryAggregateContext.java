package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 统计汇总的聚合取值（DEC-036 正模式）：一次 SQL 返回标量，不加载实体。
 *
 * <p>数据范围复用 {@link FinanceAccessContext#visible} 的同一个 Specification，
 * 权限语义与实体查询路径完全一致；逾期判定用 SQL {@code CASE WHEN} 下推，避免 Java 逐行计算。</p>
 *
 * <p>已实现并 golden 对账：<b>应收组</b>（应收/已收/未收/逾期未收）。
 * 其余分组（项目、应付、流水、收付款、期初）按同一模式**逐组实现并逐组对账**，不一次铺开。</p>
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

    private BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
    }

    /** 应收组标量结果。 */
    public record ReceivableAggregate(BigDecimal amount, BigDecimal received,
                                      BigDecimal outstanding, BigDecimal overdue) { }
}
