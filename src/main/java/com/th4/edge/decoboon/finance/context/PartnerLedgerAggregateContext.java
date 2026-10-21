package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.model.PartnerLedger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 供应商往来聚合查询：分组 SQL 出数，不加载全部应付实体。 */
@Component
public class PartnerLedgerAggregateContext {

    private final EntityManager entityManager;
    private final FinanceAccessContext accessContext;
    private final PartnerRepository partnerRepository;

    public PartnerLedgerAggregateContext(EntityManager entityManager,
                                         FinanceAccessContext accessContext,
                                         PartnerRepository partnerRepository) {
        this.entityManager = entityManager;
        this.accessContext = accessContext;
        this.partnerRepository = partnerRepository;
    }

    public Page<PartnerLedger> page(Employee current, Long partnerId, String keyword, Boolean overdueOnly,
                                    Long storeFilter, int currentPage, int pageSize) {
        List<Long> overduePartnerIds = Boolean.TRUE.equals(overdueOnly)
                ? overduePartnerIds(current, partnerId, keyword, storeFilter) : null;
        long total = countPartners(current, partnerId, keyword, storeFilter, overduePartnerIds);
        List<Object[]> rows = groupedRows(current, partnerId, keyword, storeFilter, overduePartnerIds,
                (currentPage - 1) * pageSize, pageSize);
        List<PartnerLedger> items = toLedgers(current, rows, storeFilter);
        return new PageImpl<>(items, PageRequest.of(currentPage - 1, pageSize), total);
    }

    public List<PartnerLedger> rows(Employee current, Long partnerId, String keyword, Boolean overdueOnly,
                                    Long storeFilter) {
        List<Long> overduePartnerIds = Boolean.TRUE.equals(overdueOnly)
                ? overduePartnerIds(current, partnerId, keyword, storeFilter) : null;
        return toLedgers(current,
                groupedRows(current, partnerId, keyword, storeFilter, overduePartnerIds, 0, null), storeFilter);
    }

    public PartnerLedgerSummary summary(Employee current, Long partnerId, String keyword, Boolean overdueOnly,
                                        Long storeFilter) {
        List<Long> overduePartnerIds = Boolean.TRUE.equals(overdueOnly)
                ? overduePartnerIds(current, partnerId, keyword, storeFilter) : null;
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Payable> root = query.from(Payable.class);
        Join<Payable, Partner> partner = root.join("payeePartner", JoinType.INNER);
        Expression<BigDecimal> amount = amount(cb, root);
        Expression<BigDecimal> paid = paid(cb, root);
        Expression<BigDecimal> outstanding = outstanding(cb, root);
        Expression<BigDecimal> overdue = overdue(cb, root, outstanding);

        query.multiselect(
                cb.countDistinct(partner.get("partnerId")),
                cb.sum(amount),
                cb.sum(paid),
                cb.sum(outstanding),
                cb.sum(overdue));
        query.where(predicates(cb, query, root, partner, current, partnerId, keyword, storeFilter, overduePartnerIds)
                .toArray(Predicate[]::new));

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new PartnerLedgerSummary(count(row[0]), money(row[1]), money(row[2]), money(row[3]), money(row[4]),
                LocalDateTime.now());
    }

    private List<Object[]> groupedRows(Employee current, Long partnerId, String keyword, Long storeFilter,
                                       List<Long> overduePartnerIds, int offset, Integer limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Payable> root = query.from(Payable.class);
        Join<Payable, Partner> partner = root.join("payeePartner", JoinType.INNER);
        Expression<BigDecimal> amount = amount(cb, root);
        Expression<BigDecimal> paid = paid(cb, root);
        Expression<BigDecimal> outstanding = outstanding(cb, root);
        Expression<BigDecimal> overdue = overdue(cb, root, outstanding);
        Expression<Long> pending = cb.<Long>selectCase()
                .when(cb.greaterThan(amount, paid), 1L)
                .otherwise(0L);

        query.multiselect(partner.get("partnerId"), cb.sum(amount), cb.sum(paid), cb.sum(outstanding),
                cb.sum(overdue), cb.sum(pending));
        query.where(predicates(cb, query, root, partner, current, partnerId, keyword, storeFilter, overduePartnerIds)
                .toArray(Predicate[]::new));
        query.groupBy(partner.get("partnerId"));
        query.orderBy(cb.desc(cb.sum(outstanding)), cb.asc(partner.get("partnerId")));

        TypedQuery<Object[]> typed = entityManager.createQuery(query);
        typed.setFirstResult(offset);
        if (limit != null) {
            typed.setMaxResults(limit);
        }
        return typed.getResultList();
    }

    private long countPartners(Employee current, Long partnerId, String keyword, Long storeFilter,
                               List<Long> overduePartnerIds) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Payable> root = query.from(Payable.class);
        Join<Payable, Partner> partner = root.join("payeePartner", JoinType.INNER);
        query.select(cb.countDistinct(partner.get("partnerId")));
        query.where(predicates(cb, query, root, partner, current, partnerId, keyword, storeFilter, overduePartnerIds)
                .toArray(Predicate[]::new));
        return entityManager.createQuery(query).getSingleResult();
    }

    private List<Long> overduePartnerIds(Employee current, Long partnerId, String keyword, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Payable> root = query.from(Payable.class);
        Join<Payable, Partner> partner = root.join("payeePartner", JoinType.INNER);
        List<Predicate> predicates = predicates(cb, query, root, partner, current, partnerId, keyword, storeFilter, null);
        predicates.add(cb.lessThan(root.get("dueDate"), LocalDate.now()));
        predicates.add(cb.greaterThan(amount(cb, root), paid(cb, root)));
        query.select(partner.get("partnerId")).distinct(true);
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> predicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Payable> root,
                                       Join<Payable, Partner> partner, Employee current, Long partnerId,
                                       String keyword, Long storeFilter, List<Long> overduePartnerIds) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, Payable.class).toPredicate(root, query, cb));
        if (partnerId != null) {
            predicates.add(cb.equal(partner.get("partnerId"), partnerId));
        }
        if (!StoreScope.isAllStores(storeFilter)) {
            predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
        }
        String pattern = keywordPattern(keyword);
        if (pattern != null) {
            predicates.add(cb.like(cb.lower(cb.coalesce(partner.get("name"), "")), pattern));
        }
        if (overduePartnerIds != null) {
            predicates.add(overduePartnerIds.isEmpty()
                    ? cb.disjunction()
                    : partner.get("partnerId").in(overduePartnerIds));
        }
        return predicates;
    }

    private List<PartnerLedger> toLedgers(Employee current, List<Object[]> rows, Long storeFilter) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> partnerIds = rows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, Partner> partners = new LinkedHashMap<>();
        for (Partner partner : partnerRepository.findAllById(partnerIds)) {
            partners.put(partner.getPartnerId(), partner);
        }
        Map<Long, LocalDate> nearestDueDates = nearestDueDates(current, partnerIds, storeFilter);
        List<PartnerLedger> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            PartnerLedger ledger = new PartnerLedger();
            ledger.setPartner(partners.get(id));
            ledger.setPayableAmount(money(row[1]));
            ledger.setPaidAmount(money(row[2]));
            ledger.setOutstanding(money(row[3]));
            ledger.setOverdueAmount(money(row[4]));
            ledger.setPendingCount(count(row[5]));
            ledger.setNearestDueDate(nearestDueDates.get(id));
            result.add(ledger);
        }
        return result;
    }

    private Map<Long, LocalDate> nearestDueDates(Employee current, List<Long> partnerIds, Long storeFilter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Payable> root = query.from(Payable.class);
        Join<Payable, Partner> partner = root.join("payeePartner", JoinType.INNER);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(accessContext.visible(current, Payable.class).toPredicate(root, query, cb));
        predicates.add(partner.get("partnerId").in(partnerIds));
        if (!StoreScope.isAllStores(storeFilter)) {
            predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
        }
        predicates.add(cb.greaterThan(amount(cb, root), paid(cb, root)));
        query.multiselect(partner.get("partnerId"), cb.min(root.get("dueDate")));
        query.where(predicates.toArray(Predicate[]::new));
        query.groupBy(partner.get("partnerId"));

        Map<Long, LocalDate> result = new LinkedHashMap<>();
        for (Object[] row : entityManager.createQuery(query).getResultList()) {
            result.put(((Number) row[0]).longValue(), (LocalDate) row[1]);
        }
        return result;
    }

    private Expression<BigDecimal> amount(CriteriaBuilder cb, Root<Payable> root) {
        return cb.coalesce(root.<BigDecimal>get("amount"), BigDecimal.ZERO);
    }

    private Expression<BigDecimal> paid(CriteriaBuilder cb, Root<Payable> root) {
        return cb.coalesce(root.<BigDecimal>get("paidAmount"), BigDecimal.ZERO);
    }

    private Expression<BigDecimal> outstanding(CriteriaBuilder cb, Root<Payable> root) {
        Expression<BigDecimal> amount = amount(cb, root);
        Expression<BigDecimal> paid = paid(cb, root);
        return cb.<BigDecimal>selectCase()
                .when(cb.greaterThan(amount, paid), cb.diff(amount, paid))
                .otherwise(BigDecimal.ZERO);
    }

    private Expression<BigDecimal> overdue(CriteriaBuilder cb, Root<Payable> root,
                                           Expression<BigDecimal> outstanding) {
        return cb.<BigDecimal>selectCase()
                .when(cb.and(cb.lessThan(root.get("dueDate"), LocalDate.now()),
                        cb.greaterThan(amount(cb, root), paid(cb, root))), outstanding)
                .otherwise(BigDecimal.ZERO);
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

    public record PartnerLedgerSummary(Integer partnerCount, BigDecimal payableAmount, BigDecimal paidAmount,
                                       BigDecimal outstandingAmount, BigDecimal overdueAmount,
                                       LocalDateTime asOf) { }
}
