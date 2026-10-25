package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteDesignerAggregate;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务报价按设计师聚合（DEC-036 正模式：聚合 SQL 直接投影，不加载实体、不在 Java 循环求和）。
 *
 * <p>数据范围复用 {@link DataScopeSpecification#visible}，权限语义与实体查询路径一致；
 * 预计毛利仅累计成本完整的报价（amount - cost），成本未完整报价单独计数。</p>
 */
@Component
public class QuoteFinanceAggregateContext {

    private final EntityManager entityManager;

    public QuoteFinanceAggregateContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<QuoteDesignerAggregate> aggregate(DataScope scope, QuoteStatus status) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Quote> root = query.from(Quote.class);

        Expression<BigDecimal> amount = cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
        Expression<BigDecimal> cost = cb.coalesce(root.<BigDecimal>get("cost"), BigDecimal.ZERO);
        Expression<BigDecimal> profit = cb.<BigDecimal>selectCase()
                .when(cb.isTrue(root.get("costComplete")), cb.diff(amount, cost))
                .otherwise(BigDecimal.ZERO);
        Expression<Long> incomplete = cb.<Long>selectCase()
                .when(cb.isFalse(root.get("costComplete")), 1L)
                .otherwise(0L);

        query.multiselect(
                root.get("createdBy").get("empId"),
                root.get("createdBy").get("name"),
                cb.count(root),
                cb.sum(amount),
                cb.sum(profit),
                cb.sum(incomplete));
        query.groupBy(root.get("createdBy").get("empId"), root.get("createdBy").get("name"));
        query.orderBy(cb.asc(root.get("createdBy").get("name")));

        Predicate scopePredicate = DataScopeSpecification.visible(Quote.class, scope).toPredicate(root, query, cb);
        if (status != null) {
            query.where(cb.and(scopePredicate, cb.equal(root.get("status"), status)));
        } else {
            query.where(scopePredicate);
        }

        List<Object[]> rows = entityManager.createQuery(query).getResultList();
        List<QuoteDesignerAggregate> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            QuoteDesignerAggregate aggregate = new QuoteDesignerAggregate();
            aggregate.setDesignerId((Long) row[0]);
            aggregate.setDesignerName((String) row[1]);
            aggregate.setQuoteCount(row[2] == null ? 0L : ((Number) row[2]).longValue());
            aggregate.setQuoteAmount(row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3]);
            aggregate.setProfitAmount(row[4] == null ? BigDecimal.ZERO : (BigDecimal) row[4]);
            aggregate.setCostIncompleteCount(row[5] == null ? 0L : ((Number) row[5]).longValue());
            result.add(aggregate);
        }
        return result;
    }
}
