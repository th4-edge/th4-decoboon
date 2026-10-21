package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 员工提成聚合查询，避免全量实体加载后内存汇总。 */
@Component
public class CommissionReportContext {

    private final EntityManager entityManager;
    private final PayrollAccessContext accessContext;

    public CommissionReportContext(EntityManager entityManager, PayrollAccessContext accessContext) {
        this.entityManager = entityManager;
        this.accessContext = accessContext;
    }

    public CommissionAggregate aggregate(Employee current, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<CommissionItem> root = query.from(CommissionItem.class);
        Expression<BigDecimal> commission = cb.coalesce(root.<BigDecimal>get("commissionAmount"), BigDecimal.ZERO);
        Expression<BigDecimal> accrued = cb.coalesce(root.<BigDecimal>get("accruedAmount"), BigDecimal.ZERO);
        Expression<BigDecimal> released = cb.coalesce(root.<BigDecimal>get("releasedAmount"), BigDecimal.ZERO);
        Expression<BigDecimal> paid = cb.coalesce(root.<BigDecimal>get("paidAmount"), BigDecimal.ZERO);
        query.multiselect(cb.count(root), cb.sum(commission), cb.sum(accrued), cb.sum(released), cb.sum(paid));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, CommissionItem.class).toPredicate(root, query, cb));
        if (!StoreScope.isAllStores(storeFilter)) {
            predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
        }
        query.where(cb.and(predicates.toArray(Predicate[]::new)));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new CommissionAggregate(count(row[0]), money(row[1]), money(row[2]), money(row[3]), money(row[4]),
                LocalDateTime.now());
    }

    private Integer count(Object value) {
        return value == null ? 0 : Math.toIntExact(((Number) value).longValue());
    }

    private BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
    }

    public record CommissionAggregate(Integer count, BigDecimal commissionAmount, BigDecimal accruedAmount,
                                      BigDecimal releasedAmount, BigDecimal paidAmount, LocalDateTime asOf) { }
}
